package com.jbm.cluster.api.model.job.rule;

import lombok.Data;

import javax.swing.text.Position;
import java.util.Map;

/**
 * @author scolin
 * @description 节点数据
 * @date 2025/10/22 11:13
 */
@Data
public class NodeData {
    private String id;
    private String type;
    private Map<String, Object> data;
    private String label;
    //private Position position;
}
