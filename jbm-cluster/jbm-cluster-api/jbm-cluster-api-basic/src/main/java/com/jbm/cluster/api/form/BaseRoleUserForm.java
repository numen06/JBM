package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.basic.BaseRoleUser;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("角色用户关联表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseRoleUserForm extends BaseRoleUser {
    @ApiModelProperty("分页参数")
    private PageForm pageForm;
}