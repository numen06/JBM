package com.jbm.cluster.api.entitys.message.drools;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author scolin
 * @description
 * @date 2025/8/12 19:29
 */
@Data
public class DroolsFeignTemplate implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    private static DroolsFeignTemplate droolsFeignTemplate = new DroolsFeignTemplate();

    @ApiModelProperty(value = "规则编号")
    private String ruleCode;
    @ApiModelProperty(value = "规则版本")
    private String version;
    @ApiModelProperty(value = "规则实例")
    private Map<String, Object> fact = new HashMap<>();

    public static DroolsFeignTemplate getInstance()
    {
        try {
            return (DroolsFeignTemplate)droolsFeignTemplate.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return new DroolsFeignTemplate();
    }

}
