package com.jbm.cluster.center.business.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jbm.cluster.api.entitys.gateway.GatewayRoute;
import com.jbm.cluster.api.form.GatewayRoutePageForm;
import com.jbm.cluster.center.business.GatewayRouteBusiness;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.mysql.service.impl.GatewayRouteServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class GatewayRouteBusinessImpl extends GatewayRouteServiceImpl implements GatewayRouteBusiness {

    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;

    @Override
    public void addRouteWithGatewayRefresh(GatewayRoute route) {
        normalizeRoute(route);
        addRoute(route);
        jbmClusterTemplate.refreshGateway();
    }

    @Override
    public void updateRouteWithGatewayRefresh(GatewayRoute route) {
        normalizeRoute(route);
        updateRoute(route);
        jbmClusterTemplate.refreshGateway();
    }

    @Override
    public void removeRouteWithGatewayRefresh(Long routeId) {
        removeRoute(routeId);
        jbmClusterTemplate.refreshGateway();
    }

    @Override
    public GatewayRoute buildRouteFromForm(GatewayRoutePageForm form) {
        GatewayRoute route = form != null ? form.getGatewayRoute() : null;
        if (route == null && form != null) {
            route = BeanUtil.toBean(form, GatewayRoute.class);
        }
        normalizeRoute(route);
        return route;
    }

    private void normalizeRoute(GatewayRoute route) {
        if (route != null && route.getUrl() != null && StringUtils.isNotEmpty(route.getUrl())) {
            route.setServiceId(null);
        }
    }
}