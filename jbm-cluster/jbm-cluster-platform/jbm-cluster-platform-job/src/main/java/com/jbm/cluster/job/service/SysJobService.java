package com.jbm.cluster.job.service;

import com.jbm.cluster.api.model.entitys.job.SysJob;
import com.jbm.cluster.common.exception.job.TaskException;
import com.jbm.framework.masterdata.service.IMasterDataService;
import org.quartz.SchedulerException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 定时任务调度信息信息 服务层
 *
 * @author wesley
 */
public interface SysJobService extends IMasterDataService<SysJob> {
    /**
     * 获取quartz调度器的计划任务
     *
     * @param job 调度信息
     * @return 调度任务集合
     */
    public List<SysJob> selectJobList(SysJob job);

    @Transactional(rollbackFor = Exception.class)
    int pauseJob(SysJob job) throws SchedulerException;

    @Transactional(rollbackFor = Exception.class)
    int resumeJob(SysJob job) throws SchedulerException;

    @Transactional(rollbackFor = Exception.class)
    int deleteJob(SysJob job) throws SchedulerException;

    @Transactional(rollbackFor = Exception.class)
    int changeStatus(SysJob job) throws SchedulerException;

    @Transactional(rollbackFor = Exception.class)
    void run(SysJob job) throws SchedulerException;

    /**
     * 校验cron表达式是否有效
     *
     * @param cronExpression 表达式
     * @return 结果
     */
    public boolean checkCronExpressionIsValid(String cronExpression);

    int insertJob(SysJob job) throws SchedulerException, TaskException;
}