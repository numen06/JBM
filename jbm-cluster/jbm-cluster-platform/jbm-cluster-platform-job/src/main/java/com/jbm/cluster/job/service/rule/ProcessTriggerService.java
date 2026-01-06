package com.jbm.cluster.job.service.rule;

import com.jbm.cluster.api.entitys.job.rule.ProcessInstance;
import com.jbm.cluster.api.entitys.job.rule.ProcessTrigger;
import com.jbm.framework.masterdata.service.IMasterDataService;

/**
 * @author scolin
 * @description
 * @date 2025/10/22 11:31
 */
public interface ProcessTriggerService extends IMasterDataService<ProcessTrigger> {
    ProcessTrigger findByProcessInstanceIdAndNodeId(String processInstanceId, String triggerNodeId);
}
