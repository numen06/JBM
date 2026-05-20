package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.basic.BaseAuthorityAction;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("权限功能关联表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseAuthorityActionForm extends BaseAuthorityAction {
    @ApiModelProperty("分页参数")
    private PageForm pageForm;
}