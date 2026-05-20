package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.gateway.GatewayIpLimitApi;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("网关IP限流API关联表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class GatewayIpLimitApiForm extends GatewayIpLimitApi {
    @ApiModelProperty("分页参数")
    private PageForm pageForm;
}