package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.basic.BaseAction;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("功能按钮表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseActionForm extends BaseAction {
    @ApiModelProperty("分页参数")
    private PageForm pageForm;
}