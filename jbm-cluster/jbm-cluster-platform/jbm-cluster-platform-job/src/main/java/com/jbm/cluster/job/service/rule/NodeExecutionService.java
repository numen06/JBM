package com.jbm.cluster.job.service.rule;

import com.jbm.cluster.api.entitys.job.rule.NodeExecution;
import com.jbm.cluster.api.entitys.job.rule.ProcessInstance;
import com.jbm.framework.masterdata.service.IMasterDataService;

/**
 * @author scolin
 * @description
 * @date 2025/10/22 11:30
 */
public interface NodeExecutionService extends IMasterDataService<NodeExecution> {
    NodeExecution findByProcessInstanceIdAndNodeId(String processInstanceId, String nodeId);
}
