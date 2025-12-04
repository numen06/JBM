package com.jbm.cluster.job.service.impl.rule;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.job.rule.NodeExecution;
import com.jbm.cluster.job.service.rule.NodeExecutionService;
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
        QueryWrapper<NodeExecution> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("process_instance_id", processInstanceId);
        queryWrapper.eq("node_id", nodeId);
        return this.selectEntityByWapper(queryWrapper);
    }

}
