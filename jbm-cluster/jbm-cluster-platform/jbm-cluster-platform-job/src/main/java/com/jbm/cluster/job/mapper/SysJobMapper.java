package com.jbm.cluster.job.mapper;

import com.jbm.cluster.api.model.entitys.job.SysJob;
import com.jbm.framework.masterdata.mapper.SuperMapper;

import java.util.List;

/**
 * 调度任务信息 数据层
 *
 * @author wesley
 */
public interface SysJobMapper extends SuperMapper<SysJob> {
    /**
     * 查询调度任务日志集合
     *
     * @param job 调度信息
     * @return 操作日志集合
     */
    List<SysJob> selectJobList(SysJob job);

}
