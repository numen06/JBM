package com.jbm.cluster.job.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.constants.job.MisfirePolicy;
import com.jbm.cluster.api.constants.job.ScheduleStauts;
import com.jbm.cluster.api.entitys.job.SysJob;
import com.jbm.cluster.job.exception.JobSchedulerException;
import com.jbm.cluster.job.scheduler.JobSchedulerManager;
import com.jbm.cluster.job.service.SysJobService;
import com.jbm.cluster.job.util.CronUtils;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.exceptions.job.TaskException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

/**
 * 定时任务调度信息 服务层（基于 ShedLock + ScheduledThreadPoolExecutor，不再使用 Quartz）。
 *
 * @author wesley
 */
@Slf4j
@Service
public class SysJobServiceImpl extends MasterDataServiceImpl<SysJob> implements SysJobService {

    @Resource
    private JobSchedulerManager jobSchedulerManager;

    @Resource
    private SysJobService self;

    /**
     * 是否在启动时从库全量加载并注册调度（测试或临时排障可设为 false，避免后台 Cron 链持续运行）。
     */
    @Value("${jbm.job.load-on-startup:true}")
    private boolean loadOnStartup;

    /**
     * 项目启动时，初始化定时器 主要是防止手动修改数据库导致未同步到定时任务处理（注：不能手动修改数据库ID和任务组名，否则会导致脏数据）
     */
    @PostConstruct
    public void init() {
        if (!loadOnStartup) {
            log.info("已跳过启动时定时任务全量加载（jbm.job.load-on-startup=false）");
            return;
        }
        try {
            jobSchedulerManager.reloadAllJobs();
        } catch (Exception e) {
            log.error("初始化定时任务失败", e);
            throw new IllegalStateException("初始化定时任务失败", e);
        }
    }

    @Override
    public SysJob selectJobByName(String jobName, String jobGroup) {
        QueryWrapper<SysJob> sysJobQueryWrapper = this.currentQueryWrapper();
        sysJobQueryWrapper.lambda().eq(SysJob::getJobName, jobName).eq(SysJob::getJobGroup, jobGroup);
        return this.selectEntityByWapper(sysJobQueryWrapper);
    }

    @Override
    public SysJob saveEntity(SysJob entity) {
        if (ObjectUtil.isEmpty(entity.getMisfirePolicy())) {
            entity.setMisfirePolicy(MisfirePolicy.DO_NOTHING);
        }
        if (ObjectUtil.isEmpty(entity.getConcurrent())) {
            entity.setConcurrent(false);
        }
        SysJob saved = super.saveEntity(entity);
        if (saved != null && saved.getJobId() != null) {
            jobSchedulerManager.refreshJob(this.selectById(saved.getJobId()));
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SysJob entity) {
        boolean ok = super.updateById(entity);
        if (ok && entity != null && entity.getJobId() != null) {
            jobSchedulerManager.refreshJob(this.selectById(entity.getJobId()));
        }
        return ok;
    }

    /**
     * 获取计划任务列表
     */
    @Override
    public List<SysJob> selectJobList(SysJob job) {
        return this.selectEntitys(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysJob pauseJob(SysJob job) throws JobSchedulerException {
        Long jobId = job.getJobId();
        job.setStatus(ScheduleStauts.PAUSE);
        int rows = this.baseMapper.updateById(job);
        if (rows > 0) {
            jobSchedulerManager.pauseJob(jobId);
        }
        return job;
    }

    @Override
    public List<SysJob> selectJobsByGroup(String group) {
        if (StrUtil.isBlank(group)) {
            throw new ServiceException("分组不能为空");
        }
        QueryWrapper<SysJob> sysJobQueryWrapper = this.currentQueryWrapper();
        sysJobQueryWrapper.lambda().eq(SysJob::getJobGroup, group);
        return this.selectEntitysByWapper(sysJobQueryWrapper);
    }

    @Override
    public List<SysJob> pauseGroup(String group) {
        List<SysJob> sysJobs = self.selectJobsByGroup(group);
        this.pauseJobs(sysJobs);
        return sysJobs;
    }

    @Override
    public void pauseJobs(List<SysJob> sysJobs) {
        sysJobs.forEach(sysJob -> {
            try {
                self.pauseJob(sysJob);
            } catch (JobSchedulerException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void resumeJobs(List<SysJob> sysJobs) throws JobSchedulerException {
        for (SysJob sysJob : sysJobs) {
            this.resumeJob(sysJob);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysJob resumeJob(SysJob job) throws JobSchedulerException {
        Long jobId = job.getJobId();
        if (ObjectUtil.isEmpty(jobId)) {
            throw new ServiceException("没有对应的任务ID");
        }
        job = this.selectById(jobId);
        if (ObjectUtil.isEmpty(job)) {
            throw new ServiceException("没有对应的任务");
        }
        job.setStatus(ScheduleStauts.NORMAL);
        int rows = this.baseMapper.updateById(job);
        if (rows > 0) {
            jobSchedulerManager.resumeJob(jobId);
        }
        return job;
    }

    @Override
    public boolean deleteEntity(SysJob entity) {
        try {
            return this.deleteJob(entity) > 0;
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteJob(SysJob job) throws JobSchedulerException {
        Long jobId = job.getJobId();
        int rows = this.baseMapper.deleteById(jobId);
        if (rows > 0) {
            jobSchedulerManager.unregisterJob(jobId);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysJob changeStatus(SysJob job) throws JobSchedulerException {
        if (job.getStatus() == null) {
            return job;
        }
        switch (job.getStatus()) {
            case NORMAL:
                return resumeJob(job);
            case PAUSE:
                return pauseJob(job);
            default:
                return job;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysJob run(SysJob job) throws JobSchedulerException {
        SysJob properties = this.baseMapper.selectById(job.getJobId());
        if (properties == null) {
            throw new ServiceException("没有对应的任务");
        }
        jobSchedulerManager.runOnce(properties);
        return this.resumeJob(job);
    }

    /**
     * 更新调度（等价于原 Quartz 中删除并重建触发器）。
     */
    public void updateSchedulerJob(SysJob job, String jobGroup) throws JobSchedulerException, TaskException {
        jobSchedulerManager.refreshJob(job);
    }

    @Override
    public boolean checkCronExpressionIsValid(String cronExpression) {
        return CronUtils.isValid(cronExpression);
    }

    @Override
    public int insertJob(SysJob job) throws JobSchedulerException, TaskException {
        job.setStatus(ScheduleStauts.PAUSE);
        SysJob saved = this.saveEntity(job);
        return saved == null ? 0 : 1;
    }
}
