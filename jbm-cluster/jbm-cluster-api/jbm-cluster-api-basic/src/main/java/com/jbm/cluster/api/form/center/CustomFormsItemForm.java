package com.jbm.cluster.api.form.center;

import com.jbm.cluster.api.entitys.center.CustomFormsItem;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("自定义表单项表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomFormsItemForm extends CustomFormsItem {
    @ApiModelProperty("分页参数")
    private PageForm pageForm;
}