package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("接口资源表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseApiForm extends BaseApi {
    @ApiModelProperty("分页参数")
    private PageForm pageForm;

    @ApiModelProperty("关键字，匹配编码、名称、路径、服务")
    private String keyword;
}
