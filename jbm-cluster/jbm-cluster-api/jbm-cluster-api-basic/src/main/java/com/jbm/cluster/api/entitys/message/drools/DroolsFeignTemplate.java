package com.jbm.cluster.api.entitys.message.drools;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author scolin
 * @description
 * @date 2025/8/12 19:29
 */
@Data
public class DroolsFeignTemplate {
    @ApiModelProperty(value = "规则编号")
    private String ruleCode;
    @ApiModelProperty(value = "规则版本")
    private String version;
    @ApiModelProperty(value = "规则实例")
    private Map<String, Object> fact = new HashMap<>();
}
