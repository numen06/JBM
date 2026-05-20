package com.jbm.cluster.center.business;

import com.jbm.cluster.api.entitys.gateway.GatewayRoute;
import com.jbm.cluster.api.form.GatewayRoutePageForm;
import com.jbm.cluster.common.mysql.service.GatewayRouteService;

public interface GatewayRouteBusiness extends GatewayRouteService {

    void addRouteWithGatewayRefresh(GatewayRoute route);

    void updateRouteWithGatewayRefresh(GatewayRoute route);

    void removeRouteWithGatewayRefresh(Long routeId);

    GatewayRoute buildRouteFromForm(GatewayRoutePageForm form);
}