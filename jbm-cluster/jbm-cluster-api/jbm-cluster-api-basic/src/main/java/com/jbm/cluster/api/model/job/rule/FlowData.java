package com.jbm.cluster.api.model.job.rule;

import lombok.Data;

import java.util.List;

/**
 * @author scolin
 * @description 流程数据
 * @date 2025/10/22 11:15
 */
@Data
public class FlowData {
    private List<NodeData> nodes;
    private List<EdgeData> edges;
    private Object viewport;
}
