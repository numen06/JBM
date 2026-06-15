package com.jbm.cluster.api.model.basic;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织用户树节点，供 TreeSelect 使用
 */
@Data
@NoArgsConstructor
@ApiModel("组织用户树节点")
public class OrgUserTreeNode {

    @ApiModelProperty("节点唯一标识")
    private String key;

    @ApiModelProperty("显示名称")
    private String title;

    @ApiModelProperty("节点值")
    private String value;

    @ApiModelProperty("节点类型：org / user")
    private String nodeType;

    @ApiModelProperty("组织ID")
    private Long orgId;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("是否禁用（部门节点不可选）")
    private Boolean disabled;

    @ApiModelProperty("子节点")
    private List<OrgUserTreeNode> children = new ArrayList<>();
}
