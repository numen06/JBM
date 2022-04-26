package com.jbm.cluster.center.service;

import com.jbm.cluster.api.form.GatewayRoutePageForm;
import com.jbm.cluster.api.entitys.gateway.GatewayRoute;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.usage.paging.DataPaging;

import java.util.List;

/**
 * 路由管理
 *
 * @author wesley.zhang
 */
public interface GatewayRouteService extends IMasterDataService<GatewayRoute> {
    List<String> getMicroServices();

    /**
     * 分页查询
     *
     * @param gatewayRoutePageForm
     * @return
     */
    DataPaging<GatewayRoute> findListPage(GatewayRoutePageForm gatewayRoutePageForm);

    /**
     * 查询可用路由列表
     *
     * @return
     */
    List<GatewayRoute> findRouteList();

    /**
     * 获取路由信息
     *
     * @param routeId
     * @return
     */
    GatewayRoute getRoute(Long routeId);

    /**
     * 添加路由
     *
     * @param route
     */
    void addRoute(GatewayRoute route);

    /**
     * 更新路由
     *
     * @param route
     */
    void updateRoute(GatewayRoute route);

    /**
     * 删除路由
     *
     * @param routeId
     */
    void removeRoute(Long routeId);

    /**
     * 是否存在
     *
     * @param routeName
     * @return
     */
    Boolean isExist(String routeName);
}
