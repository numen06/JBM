package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.basic.BaseOpenUser;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("开放用户表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseOpenUserForm extends BaseOpenUser {
    @ApiModelProperty("分页参数")
    private PageForm pageForm;
}