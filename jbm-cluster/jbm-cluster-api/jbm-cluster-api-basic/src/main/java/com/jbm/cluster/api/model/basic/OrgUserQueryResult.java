package com.jbm.cluster.api.model.basic;

import com.jbm.cluster.api.entitys.basic.BaseUser;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织用户查询结果
 */
@Data
@NoArgsConstructor
@ApiModel("组织用户查询结果")
public class OrgUserQueryResult {

    @ApiModelProperty("用户列表")
    private List<BaseUser> users = new ArrayList<>();

    @ApiModelProperty("部门-用户树")
    private List<OrgUserTreeNode> tree = new ArrayList<>();

    public OrgUserQueryResult(List<BaseUser> users, List<OrgUserTreeNode> tree) {
        this.users = users;
        this.tree = tree;
    }
}
