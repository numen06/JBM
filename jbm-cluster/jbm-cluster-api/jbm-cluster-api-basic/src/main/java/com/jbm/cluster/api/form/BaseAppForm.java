package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("应用表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseAppForm extends BaseApp {
    @ApiModelProperty("aid")
    private Long aid;
    @ApiModelProperty("分页参数")
    private PageForm pageForm;
}