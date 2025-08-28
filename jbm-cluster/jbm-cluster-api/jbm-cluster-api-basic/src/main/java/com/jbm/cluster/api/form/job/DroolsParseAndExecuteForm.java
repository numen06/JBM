package com.jbm.cluster.api.form.job;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author scolin
 * @description
 * @date 2025/8/26 15:14
 */
@ApiModel("drools解析和执行表单")
@Data
public class DroolsParseAndExecuteForm {
    @ApiModelProperty("原始json内容")
    private String originalJson;
    @ApiModelProperty("节点ID")
    private String nodeId;
    @ApiModelProperty("实例")
    private String fact;
}
