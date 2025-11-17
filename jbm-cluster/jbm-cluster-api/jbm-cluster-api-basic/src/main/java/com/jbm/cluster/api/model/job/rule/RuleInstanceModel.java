package com.jbm.cluster.api.model.job.rule;

import com.jbm.cluster.api.entitys.job.rule.NodeExecution;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author scolin
 * @description 规则/流程实例模版
 * @date 2025/11/17 16:43
 */
@Data
@ApiModel("规则/流程实例模版")
public class RuleInstanceModel {
    @ApiModelProperty("实例id")
    private String id;
    @ApiModelProperty("规则id")
    private String ruleId;
    @ApiModelProperty("规则名称")
    private String ruleName;
    @ApiModelProperty("规则编号")
    private String ruleCode;
//    @ApiModelProperty("规则版本")
//    private String version;
//    @ApiModelProperty("规则组")
//    private String ruleGroup;
//    @ApiModelProperty("规则状态")
//    private String ruleStatus;
    @ApiModelProperty("规则内容")
    private FlowData ruleContent;
    @ApiModelProperty("实例状态")
    private String status;
    @ApiModelProperty("实例输入参数")
    private String inputParams;
    @ApiModelProperty("实例输出参数")
    private String outputParams;
    @ApiModelProperty("实例创建时间")
    private String createdAt;
    @ApiModelProperty("实例更新时间")
    private String updatedAt;
    @ApiModelProperty("实例执行各节点信息")
    private List<NodeExecution> nodeExecutions;
}
