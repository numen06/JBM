package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.jbm.cluster.api.entitys.gateway.GatewayRoute;
import com.jbm.cluster.api.form.GatewayRoutePageForm;
import com.jbm.cluster.common.mysql.service.GatewayRouteService;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.env.Environment;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wesley.zhang
 */
@Slf4j
@Service
public class GatewayRouteServiceImpl extends MasterDataServiceImpl<GatewayRoute> implements GatewayRouteService {

    private static final String PLATFORM_PREFIX = "jbm-cluster-platform-";
    private static final String GATEWAY_SHORT_NAME = "gateway";
    private static final String AUTO_SYNC_DESC_PREFIX = "Auto synced from discovery: ";
    private static final Map<String, String> DEFAULT_SERVICE_ALIASES = defaultServiceAliases();

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
    @Autowired
    private Environment environment;


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
        syncDiscoveryRoutes();
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
        syncDiscoveryRoutes();
        QueryWrapper<GatewayRoute> queryWrapper = new QueryWrapper();
        queryWrapper.select(ROUTE_COLUMNS);
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

    @Transactional(rollbackFor = Exception.class)
    public synchronized void syncDiscoveryRoutes() {
        List<String> services;
        try {
            services = discoveryClient.getServices();
        } catch (Exception e) {
            log.warn("Sync discovery routes failed: unable to read service list", e);
            return;
        }
        if (services == null || services.isEmpty()) {
            return;
        }
        for (String serviceId : services) {
            if (StrUtil.isBlank(serviceId) || !StrUtil.startWith(serviceId, PLATFORM_PREFIX)) {
                continue;
            }
            String alias = routeAliasFor(serviceId);
            if (StrUtil.isBlank(alias) || StrUtil.equals(alias, GATEWAY_SHORT_NAME)) {
                continue;
            }
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            if (instances == null || instances.isEmpty()) {
                continue;
            }
            String path = "/" + alias + "/**";
            if (normalizeExistingDiscoveryRoute(serviceId, alias, path)) {
                continue;
            }
            if (routeExists(alias, path)) {
                continue;
            }
            GatewayRoute route = new GatewayRoute();
            route.setRouteName(alias);
            route.setPath(path);
            route.setServiceId(serviceId);
            route.setUrl("lb://" + serviceId);
            route.setStripPrefix(1);
            route.setRetryable(JbmConstants.DISABLED);
            route.setStatus(JbmConstants.ENABLED);
            route.setIsPersist(JbmConstants.DISABLED);
            route.setRouteDesc(AUTO_SYNC_DESC_PREFIX + serviceId);
            save(route);
            log.info("Auto synced discovery gateway route routeName={}, path={}, serviceId={}", alias, path, serviceId);
        }
    }

    private boolean normalizeExistingDiscoveryRoute(String serviceId, String routeName, String path) {
        QueryWrapper<GatewayRoute> queryWrapper = new QueryWrapper<>();
        queryWrapper.select(ROUTE_COLUMNS);
        queryWrapper.lambda().eq(GatewayRoute::getServiceId, serviceId);
        List<GatewayRoute> routes = list(queryWrapper);
        if (routes == null || routes.isEmpty()) {
            return false;
        }
        for (GatewayRoute route : routes) {
            if (!isAutoSyncedDiscoveryRoute(route)) {
                continue;
            }
            if (StrUtil.equals(route.getRouteName(), routeName) && StrUtil.equals(route.getPath(), path)) {
                return true;
            }
            route.setRouteName(routeName);
            route.setPath(path);
            route.setRouteDesc(AUTO_SYNC_DESC_PREFIX + serviceId);
            updateById(route);
            log.info("Normalized discovery gateway route routeName={}, path={}, serviceId={}", routeName, path, serviceId);
            return true;
        }
        return false;
    }

    private boolean isAutoSyncedDiscoveryRoute(GatewayRoute route) {
        return route != null && StrUtil.startWith(route.getRouteDesc(), AUTO_SYNC_DESC_PREFIX);
    }

    private boolean routeExists(String routeName, String path) {
        QueryWrapper<GatewayRoute> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(GatewayRoute::getRouteName, routeName).or().eq(GatewayRoute::getPath, path);
        return count(queryWrapper) > 0;
    }

    private String routeAliasFor(String serviceId) {
        String alias = DEFAULT_SERVICE_ALIASES.get(serviceId);
        if (StrUtil.isNotBlank(alias)) {
            return alias;
        }
        String shortName = StrUtil.removePrefix(serviceId, PLATFORM_PREFIX);
        String profileName = activeProfileName();
        if (StrUtil.isNotBlank(profileName) && StrUtil.endWith(shortName, "-" + profileName)) {
            String profileFreeShortName = StrUtil.removeSuffix(shortName, "-" + profileName);
            alias = DEFAULT_SERVICE_ALIASES.get(PLATFORM_PREFIX + profileFreeShortName);
            return StrUtil.blankToDefault(alias, profileFreeShortName);
        }
        for (Map.Entry<String, String> entry : DEFAULT_SERVICE_ALIASES.entrySet()) {
            String defaultShortName = StrUtil.removePrefix(entry.getKey(), PLATFORM_PREFIX);
            if (StrUtil.startWith(shortName, defaultShortName + "-")) {
                return entry.getValue();
            }
        }
        return shortName;
    }

    private String activeProfileName() {
        String profileName = environment.getProperty("profile.name");
        if (StrUtil.isNotBlank(profileName)) {
            return profileName;
        }
        String active = environment.getProperty("spring.profiles.active");
        if (StrUtil.isNotBlank(active)) {
            return active.split(",")[0].trim();
        }
        return "";
    }

    private static Map<String, String> defaultServiceAliases() {
        Map<String, String> aliases = new HashMap<>(16);
        aliases.put(JbmClusterConstants.BASE_SERVER, "center");
        aliases.put(JbmClusterConstants.AUTH_SERVER, "auth");
        aliases.put(JbmClusterConstants.DOC_SERVER, "doc");
        aliases.put(JbmClusterConstants.PUSH_SERVER, "push");
        aliases.put(JbmClusterConstants.LOG_SERVER, "logs");
        aliases.put(JbmClusterConstants.BIGSCREEN_SERVER, "bigscreen");
        aliases.put(JbmClusterConstants.JOB_SERVER, "job");
        aliases.put(JbmClusterConstants.WEIXIN_SERVER, "weixin");
        return Collections.unmodifiableMap(aliases);
    }
}
