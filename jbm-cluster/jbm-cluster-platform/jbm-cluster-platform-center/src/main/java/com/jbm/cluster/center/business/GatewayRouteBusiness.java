package com.jbm.cluster.center.business;

import com.jbm.cluster.api.entitys.gateway.GatewayRoute;
import com.jbm.cluster.api.form.GatewayRoutePageForm;

public interface GatewayRouteBusiness {

    void addRouteWithGatewayRefresh(GatewayRoute route);

    void updateRouteWithGatewayRefresh(GatewayRoute route);

    void removeRouteWithGatewayRefresh(Long routeId);

    GatewayRoute buildRouteFromForm(GatewayRoutePageForm form);
}
