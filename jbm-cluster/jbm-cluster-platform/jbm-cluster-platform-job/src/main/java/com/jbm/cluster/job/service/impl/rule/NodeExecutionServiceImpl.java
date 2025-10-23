package com.jbm.cluster.job.service.impl.rule;

import com.jbm.cluster.api.entitys.job.rule.NodeExecution;
import com.jbm.cluster.api.entitys.job.rule.ProcessInstance;
import com.jbm.cluster.job.service.rule.NodeExecutionService;
import com.jbm.cluster.job.service.rule.ProcessInstanceService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author scolin
 * @description
 * @date 2025/10/22 15:56
 */
@Service
public class NodeExecutionServiceImpl extends MasterDataServiceImpl<NodeExecution> implements NodeExecutionService {
    @Override
    public NodeExecution findByProcessInstanceIdAndNodeId(String processInstanceId, String nodeId) {
        return null;
    }

}
