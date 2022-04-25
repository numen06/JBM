package com.jbm.cluster.api.form;

import com.jbm.cluster.api.model.entity.BaseAuthorityRole;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-27 22:31
 **/
@ApiModel("角色权限设置表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseAuthorityRoleForm extends BaseAuthorityRole {

    @ApiModelProperty("权限ID数组")
    private String[] authorityIds;
}
