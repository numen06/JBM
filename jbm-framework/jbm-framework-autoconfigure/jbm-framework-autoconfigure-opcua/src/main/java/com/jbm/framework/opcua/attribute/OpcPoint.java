package com.jbm.framework.opcua.attribute;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpcPoint {

    /**
     * 命名空间
     */
    private Integer namespace;

    /**
     * 点位名称
     */
    private String tagName;
    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 别名
     */
    private String alias;
    /**
     * 数值
     */
    private Object value;

    public OpcPoint(String alias) {
        this.alias = alias;
    }

    public OpcPoint(Integer namespace, String tagName, String dataType) {
        this.namespace = namespace;
        this.tagName = tagName;
        this.dataType = dataType;
    }

    public String print() {
        return StrUtil.concat(true, namespace.toString(), ":", tagName, ":", dataType);
    }


}
