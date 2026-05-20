package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.basic.BaseUserConfig;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("用户配置表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseUserConfigForm extends BaseUserConfig {
    @ApiModelProperty("分页参数")
    private PageForm pageForm;
}