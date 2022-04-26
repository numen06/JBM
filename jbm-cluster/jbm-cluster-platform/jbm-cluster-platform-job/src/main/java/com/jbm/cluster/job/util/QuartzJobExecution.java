package com.jbm.cluster.job.util;

import org.quartz.JobExecutionContext;

import com.jbm.cluster.api.entitys.job.SysJob;

/**
 * 定时任务处理（允许并发执行）
 * 
 * @author wesley
 *
 */
public class QuartzJobExecution extends AbstractQuartzJob
{
    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception
    {
        JobInvokeUtil.invokeMethod(sysJob);
    }
}
