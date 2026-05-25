package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.jbm.cluster.api.entitys.gateway.GatewayRoute;
import com.jbm.cluster.api.form.GatewayRoutePageForm;
import com.jbm.cluster.common.mysql.service.GatewayRouteService;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
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
public class GatewayRouteServiceImpl extends MasterDataServiceImpl<GatewayRoute> implements GatewayRouteService {

    private static final String[] ROUTE_COLUMNS = {
            "route_id",
            "route_name",
            "path",
            "service_id",
            "url",
            "strip_prefix",
            "retryable",
            "status",
            "is_persist",
            "route_desc",
            "create_time",
            "update_time",
            "extend_data"
    };

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
        PageForm pageForm = gatewayRoutePageForm != null && gatewayRoutePageForm.getPageForm() != null
                ? gatewayRoutePageForm.getPageForm()
                : new PageForm();
        GatewayRoute route = gatewayRoutePageForm != null ? gatewayRoutePageForm.getGatewayRoute() : null;
        QueryWrapper<GatewayRoute> queryWrapper = new QueryWrapper<>();
        queryWrapper.select(ROUTE_COLUMNS);
        if (route != null) {
            queryWrapper.lambda().eq(ObjectUtils.isNotEmpty(route.getStatus()), GatewayRoute::getStatus, route.getStatus());
            if (StrUtil.isNotBlank(route.getRouteName())) {
                String kw = route.getRouteName();
                queryWrapper.lambda().and(w -> w.likeRight(GatewayRoute::getRouteName, kw)
                        .or().likeRight(GatewayRoute::getPath, kw)
                        .or().likeRight(GatewayRoute::getServiceId, kw));
            } else {
                queryWrapper.lambda()
                        .likeRight(StrUtil.isNotBlank(route.getPath()), GatewayRoute::getPath, route.getPath())
                        .likeRight(StrUtil.isNotBlank(route.getServiceId()), GatewayRoute::getServiceId, route.getServiceId());
            }
        }
        queryWrapper.orderByDesc("create_time");
        return selectEntitys(PageParams.from(pageForm), queryWrapper);
    }

    /**
     * 查询可用路由列表
     *
     * @return
     */
    @Override
    public List<GatewayRoute> findRouteList() {
        QueryWrapper<GatewayRoute> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(GatewayRoute::getStatus, JbmConstants.ENABLED);
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
        QueryWrapper<GatewayRoute> queryWrapper = new QueryWrapper<>();
        queryWrapper.select(ROUTE_COLUMNS);
        queryWrapper.lambda().eq(GatewayRoute::getRouteId, routeId);
        return getOne(queryWrapper);
    }

    /**
     * 添加路由
     *
     * @param route
     */
    @Override
    public void addRoute(GatewayRoute route) {
        if (StringUtils.isBlank(route.getPath())) {
            throw new ServiceException(String.format("path不能为空!"));
        }
        if (isExist(route.getRouteName())) {
            throw new ServiceException(String.format("路由名称已存在!"));
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
            throw new ServiceException(String.format("path不能为空"));
        }
        GatewayRoute saved = getRoute(route.getRouteId());
        if (saved == null) {
            throw new ServiceException("路由信息不存在!");
        }
        if (saved != null && saved.getIsPersist().equals(JbmConstants.ENABLED)) {
            throw new ServiceException(String.format("保留数据,不允许修改"));
        }
        if (!saved.getRouteName().equals(route.getRouteName())) {
            // 和原来不一致重新检查唯一性
            if (isExist(route.getRouteName())) {
                throw new ServiceException("路由名称已存在!");
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
        if (saved != null && saved.getIsPersist().equals(JbmConstants.ENABLED)) {
            throw new ServiceException(String.format("保留数据,不允许删除"));
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
        Long count = count(queryWrapper);
        return count > 0;
    }
}
