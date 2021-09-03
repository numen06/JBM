package com.jbm.cluster.center.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.constants.BaseConstants;
import com.jbm.cluster.api.form.GatewayRoutePageForm;
import com.jbm.cluster.api.model.entity.GatewayRoute;
import com.jbm.cluster.center.service.GatewayRouteService;
import com.jbm.cluster.common.exception.OpenAlertException;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author wesley.zhang
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class GatewayRouteServiceImpl extends MasterDataServiceImpl<GatewayRoute> implements GatewayRouteService {


    @Autowired
    private DiscoveryClient discoveryClient;


    /**
     * 查询所有微服务
     *
     * @return
     */
    @Override
    public List<String> getMicroServices() {
        return discoveryClient.getServices();
    }


    /**
     * 分页查询
     *
     * @param gatewayRoutePageForm
     * @return
     */
    @Override
    public DataPaging<GatewayRoute> findListPage(GatewayRoutePageForm gatewayRoutePageForm) {
        return this.selectEntitys(gatewayRoutePageForm.getGatewayRoute(), gatewayRoutePageForm.getPageForm());
    }

    /**
     * 查询可用路由列表
     *
     * @return
     */
    @Override
    public List<GatewayRoute> findRouteList() {
        QueryWrapper<GatewayRoute> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(GatewayRoute::getStatus, BaseConstants.ENABLED);
        List<GatewayRoute> list = list(queryWrapper);
        return list;
    }

    /**
     * 获取路由信息
     *
     * @param routeId
     * @return
     */
    @Override
    public GatewayRoute getRoute(Long routeId) {
        return getById(routeId);
    }

    /**
     * 添加路由
     *
     * @param route
     */
    @Override
    public void addRoute(GatewayRoute route) {
        if (StringUtils.isBlank(route.getPath())) {
            throw new OpenAlertException(String.format("path不能为空!"));
        }
        if (isExist(route.getRouteName())) {
            throw new OpenAlertException(String.format("路由名称已存在!"));
        }
        route.setIsPersist(0);
        save(route);
    }

    /**
     * 更新路由
     *
     * @param route
     */
    @Override
    public void updateRoute(GatewayRoute route) {
        if (StringUtils.isBlank(route.getPath())) {
            throw new OpenAlertException(String.format("path不能为空"));
        }
        GatewayRoute saved = getRoute(route.getRouteId());
        if (saved == null) {
            throw new OpenAlertException("路由信息不存在!");
        }
        if (saved != null && saved.getIsPersist().equals(BaseConstants.ENABLED)) {
            throw new OpenAlertException(String.format("保留数据,不允许修改"));
        }
        if (!saved.getRouteName().equals(route.getRouteName())) {
            // 和原来不一致重新检查唯一性
            if (isExist(route.getRouteName())) {
                throw new OpenAlertException("路由名称已存在!");
            }
        }
        updateById(route);
    }

    /**
     * 删除路由
     *
     * @param routeId
     */
    @Override
    public void removeRoute(Long routeId) {
        GatewayRoute saved = getRoute(routeId);
        if (saved != null && saved.getIsPersist().equals(BaseConstants.ENABLED)) {
            throw new OpenAlertException(String.format("保留数据,不允许删除"));
        }
        removeById(routeId);
    }

    /**
     * 查询地址是否存在
     *
     * @param routeName
     */
    @Override
    public Boolean isExist(String routeName) {
        QueryWrapper<GatewayRoute> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(GatewayRoute::getRouteName, routeName);
        int count = count(queryWrapper);
        return count > 0;
    }
}
