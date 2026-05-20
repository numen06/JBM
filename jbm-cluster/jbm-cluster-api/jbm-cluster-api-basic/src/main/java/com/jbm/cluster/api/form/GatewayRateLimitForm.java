package com.jbm.cluster.api.form;

import com.jbm.cluster.api.entitys.gateway.GatewayRateLimit;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("网关流量限流策略表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class GatewayRateLimitForm extends GatewayRateLimit {
    @ApiModelProperty("分页参数")
    private PageForm pageForm;
}