package com.jbm.cluster.api.form;

import com.jbm.cluster.api.model.entity.GatewayRoute;
import com.jbm.framework.usage.form.PageSearchForm;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel("路由分页查询")
@Data
@NoArgsConstructor
public class GatewayRoutePageForm extends PageSearchForm {

    private GatewayRoute gatewayRoute;

}
