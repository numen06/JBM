package com.jbm.cluster.api.model.job.rule;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

/**
 * @author scolin
 * @description 直接执行流程JSON请求（不使用本地规则定义）
 * @date 2025/11/25
 */
@Data
@ApiModel("直接执行流程JSON请求")
public class ExecuteProcessByJsonRequest {
    @ApiModelProperty("流程定义的JSON内容")
    private String ruleContent;

    @ApiModelProperty("规则名称")
    private String ruleName;

    @ApiModelProperty("输入参数")
    private Map<String, Object> inputParams;

    @ApiModelProperty("流程实例ID")
    private String processInstanceId;
}
