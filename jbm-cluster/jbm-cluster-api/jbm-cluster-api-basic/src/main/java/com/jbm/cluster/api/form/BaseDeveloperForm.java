package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.basic.BaseDeveloper;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("开发者表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseDeveloperForm extends BaseDeveloper {
    @ApiModelProperty("分页参数")
    private PageForm pageForm;
}