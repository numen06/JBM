package com.jbm.cluster.job.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.constants.job.MisfirePolicy;
import com.jbm.cluster.api.constants.job.ScheduleConstants;
import com.jbm.cluster.api.constants.job.ScheduleStauts;
import com.jbm.cluster.api.entitys.job.SysJob;
import com.jbm.cluster.job.service.SysJobService;
import com.jbm.cluster.job.util.CronUtils;
import com.jbm.cluster.job.util.ScheduleUtils;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.exceptions.job.TaskException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 定时任务调度信息 服务层
 *
 * @author wesley
 */
@Slf4j
@Service
public class SysJobServiceImpl extends MasterDataServiceImpl<SysJob> implements SysJobService {
    @Autowired
    private Scheduler scheduler;

    /**
     * 项目启动时，初始化定时器 主要是防止手动修改数据库导致未同步到定时任务处理（注：不能手动修改数据库ID和任务组名，否则会导致脏数据）
     */
    @PostConstruct
    public void init() throws SchedulerException, TaskException {
        scheduler.clear();
        List<SysJob> jobList = this.selectAll();
        for (SysJob job : jobList) {
            try {
                ScheduleUtils.createScheduleJob(scheduler, job);
            } catch (Exception e) {
                log.error("初始化任务失败:{}", job.getJobName(), e);
            }
        }
    }

    @Override
    public SysJob saveEntity(SysJob entity) {
        if (ObjectUtil.isEmpty(entity.getMisfirePolicy()))
            entity.setMisfirePolicy(MisfirePolicy.DEFAULT);
        if (ObjectUtil.isEmpty(entity.getConcurrent()))
            entity.setConcurrent(false);
        return super.saveEntity(entity);
    }

    /**
     * 获取quartz调度器的计划任务列表
     *
     * @param job 调度信息
     * @return
     */
    @Override
    public List<SysJob> selectJobList(SysJob job) {
        return this.selectEntitys(job);
    }


    /**
     * 暂停任务
     *
     * @param job 调度信息
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysJob pauseJob(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        job.setStatus(ScheduleStauts.PAUSE);
        int rows = this.baseMapper.updateById(job);
        if (rows > 0) {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return job;
    }

    /**
     * 恢复任务
     *
     * @param job 调度信息
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysJob resumeJob(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        job.setStatus(ScheduleStauts.NORMAL);
        int rows = this.baseMapper.updateById(job);
        if (rows > 0) {
            scheduler.resetTriggerFromErrorState(ScheduleUtils.getTriggerKey(jobId, jobGroup));
            scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));
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

    /**
     * 删除任务后，所对应的trigger也将被删除
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteJob(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        int rows = this.baseMapper.deleteById(jobId);
        if (rows > 0) {
            scheduler.deleteJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }


    /**
     * 任务调度状态修改
     *
     * @param job 调度信息
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysJob changeStatus(SysJob job) throws SchedulerException {
        int rows = 0;
        switch (job.getStatus()) {
            case NORMAL:
                return resumeJob(job);
            case PAUSE:
                return pauseJob(job);
        }
        return job;
    }

    /**
     * 立即运行任务
     *
     * @param job 调度信息
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysJob run(SysJob job) throws SchedulerException {
        Long jobId = job.getJobId();
        SysJob properties = this.baseMapper.selectById(job.getJobId());
        String jobGroup = properties.getJobGroup();
        // 参数
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduleConstants.TASK_PROPERTIES, properties);
        scheduler.triggerJob(ScheduleUtils.getJobKey(jobId, jobGroup), dataMap);
//        Trigger.TriggerState triggerState = scheduler.getTriggerState(ScheduleUtils.getTriggerKey(jobId, jobGroup));
        return this.resumeJob(job);
//        job.setStatus(Trigger.TriggerState.NORMAL.equals(triggerState) ? ScheduleStauts.NORMAL : ScheduleStauts.PAUSE);
//        return this.saveEntity(job);
    }


    /**
     * 更新任务
     *
     * @param job      任务对象
     * @param jobGroup 任务组名
     */
    public void updateSchedulerJob(SysJob job, String jobGroup) throws SchedulerException, TaskException {
        Long jobId = job.getJobId();
        // 判断是否存在
        JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(jobKey);
        }
        ScheduleUtils.createScheduleJob(scheduler, job);
    }

    /**
     * 校验cron表达式是否有效
     *
     * @param cronExpression 表达式
     * @return 结果
     */
    @Override
    public boolean checkCronExpressionIsValid(String cronExpression) {
        return CronUtils.isValid(cronExpression);
    }

    @Override
    public int insertJob(SysJob job) throws SchedulerException, TaskException {
        job.setStatus(ScheduleStauts.PAUSE);
        int rows = this.saveEntity(job) == null ? 0 : 1;
        if (rows > 0) {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
        return rows;
    }
}