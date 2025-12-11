package com.jbm.cluster.api.model.job.rule;

import lombok.Data;

/**
 * @author scolin
 * @description 边数据
 * @date 2025/10/22 11:14
 */
@Data
public class EdgeData {
    private String id;
    private String source;
    private String target;
    private String sourceHandle;
    private String targetHandle;
    private String type;
}
