package com.jbm.cluster.job.service;

import com.jbm.cluster.api.entitys.job.SysJob;
import com.jbm.cluster.job.exception.JobSchedulerException;
import com.jbm.framework.exceptions.job.TaskException;
import com.jbm.framework.masterdata.service.IMasterDataService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 定时任务调度信息信息 服务层
 *
 * @author wesley
 */
public interface SysJobService extends IMasterDataService<SysJob> {
    SysJob selectJobByName(String jobName, String jobGroup);

    /**
     * 获取计划任务列表
     *
     * @param job 调度信息
     * @return 调度任务集合
     */
    List<SysJob> selectJobList(SysJob job);

    @Transactional(rollbackFor = Exception.class)
    SysJob pauseJob(SysJob job) throws JobSchedulerException;

    List<SysJob> selectJobsByGroup(String group);

    List<SysJob> pauseGroup(String group);

    void pauseJobs(List<SysJob> sysJobs);

    void resumeJobs(List<SysJob> sysJobs) throws JobSchedulerException;

    @Transactional(rollbackFor = Exception.class)
    SysJob resumeJob(SysJob job) throws JobSchedulerException;

    @Transactional(rollbackFor = Exception.class)
    int deleteJob(SysJob job) throws JobSchedulerException;

    @Transactional(rollbackFor = Exception.class)
    SysJob changeStatus(SysJob job) throws JobSchedulerException;

    @Transactional(rollbackFor = Exception.class)
    SysJob run(SysJob job) throws JobSchedulerException;

    /**
     * 校验cron表达式是否有效
     *
     * @param cronExpression 表达式
     * @return 结果
     */
    boolean checkCronExpressionIsValid(String cronExpression);

    int insertJob(SysJob job) throws JobSchedulerException, TaskException;
}
