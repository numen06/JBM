package com.jbm.cluster.job.mapper;

import com.jbm.cluster.api.entitys.job.SysJobLog;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 调度任务日志信息 数据层
 *
 * @author wesley
 */
public interface SysJobLogMapper extends SuperMapper<SysJobLog> {
    /**
     * 获取quartz调度器日志的计划任务
     *
     * @param jobLog 调度日志信息
     * @return 调度任务日志集合
     */
    List<SysJobLog> selectJobLogList(SysJobLog jobLog);


    /**
     * 清空任务日志
     */
    @Update("truncate table sys_job_log")
    void cleanJobLog();
}
