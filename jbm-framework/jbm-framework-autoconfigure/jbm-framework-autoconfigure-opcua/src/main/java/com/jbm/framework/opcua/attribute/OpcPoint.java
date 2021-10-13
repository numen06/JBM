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

    private Integer namespace;

    private String tagName;

    private String dataType;

    private Object value;

    public OpcPoint(Integer namespace, String tagName, String dataType) {
        this.namespace = namespace;
        this.tagName = tagName;
        this.dataType = dataType;
    }

    public String print() {
        return StrUtil.concat(true, namespace.toString(), ":", tagName, ":", dataType);
    }


}
