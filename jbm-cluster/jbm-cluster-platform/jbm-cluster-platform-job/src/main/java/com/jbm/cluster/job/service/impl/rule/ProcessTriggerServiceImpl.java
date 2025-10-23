package com.jbm.cluster.job.service.impl.rule;

import com.jbm.cluster.api.entitys.job.rule.NodeExecution;
import com.jbm.cluster.api.entitys.job.rule.ProcessTrigger;
import com.jbm.cluster.job.service.rule.NodeExecutionService;
import com.jbm.cluster.job.service.rule.ProcessTriggerService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author scolin
 * @description
 * @date 2025/10/22 16:27
 */
@Service
public class ProcessTriggerServiceImpl extends MasterDataServiceImpl<ProcessTrigger> implements ProcessTriggerService {
    @Override
    public ProcessTrigger findByProcessInstanceIdAndNodeId(String processInstanceId, String triggerNodeId) {
        return null;
    }
}
