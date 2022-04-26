package com.jbm.cluster.job.service;

import com.jbm.cluster.api.entitys.job.SysJobLog;
import com.jbm.framework.masterdata.service.IMasterDataService;

import java.util.List;

/**
 * 定时任务调度日志信息信息 服务层
 *
 * @author wesley
 */
public interface SysJobLogService extends IMasterDataService<SysJobLog> {
    /**
     * 获取quartz调度器日志的计划任务
     *
     * @param jobLog 调度日志信息
     * @return 调度任务日志集合
     */
    public List<SysJobLog> selectJobLogList(SysJobLog jobLog);


    /**
     * 清空任务日志
     */
    public void cleanJobLog();
}
