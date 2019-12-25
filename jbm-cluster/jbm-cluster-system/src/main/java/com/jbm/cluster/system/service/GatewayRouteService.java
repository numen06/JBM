package com.jbm.cluster.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jbm.cluster.api.model.entity.GatewayRoute;
import com.jbm.framework.masterdata.service.IMasterDataService;

import java.util.List;

/**
 * 路由管理
 *
 * @author liuyadu
 */
public interface GatewayRouteService extends IMasterDataService<GatewayRoute> {
    /**
     * 分页查询
     *
     * @param pageForm
     * @return
     */
    DataPaging<GatewayRoute> findListPage(PageForm pageForm);

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
