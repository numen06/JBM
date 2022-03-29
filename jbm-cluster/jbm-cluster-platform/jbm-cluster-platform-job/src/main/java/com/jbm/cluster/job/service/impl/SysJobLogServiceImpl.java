package com.jbm.cluster.job.service.impl;

import com.jbm.cluster.api.model.entitys.job.SysJobLog;
import com.jbm.cluster.job.mapper.SysJobLogMapper;
import com.jbm.cluster.job.service.SysJobLogService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 定时任务调度日志信息 服务层
 *
 * @author wesley
 */
@Service
public class SysJobLogServiceImpl extends MasterDataServiceImpl<SysJobLog> implements SysJobLogService {
    @Autowired
    private SysJobLogMapper jobLogMapper;

    /**
     * 获取quartz调度器日志的计划任务
     *
     * @param jobLog 调度日志信息
     * @return 调度任务日志集合
     */
    @Override
    public List<SysJobLog> selectJobLogList(SysJobLog jobLog) {
        return jobLogMapper.selectJobLogList(jobLog);
    }


    /**
     * 清空任务日志
     */
    @Override
    public void cleanJobLog() {
        jobLogMapper.cleanJobLog();
    }
}
