package com.jbm.cluster.platform.gateway.locator;

import com.google.common.collect.Lists;
import com.jbm.cluster.api.entitys.auth.AuthorityResource;
import com.jbm.cluster.api.model.IpLimitApi;
import com.jbm.cluster.api.service.fegin.client.BaseApiServiceClient;
import com.jbm.cluster.api.service.fegin.client.BaseAppServiceClient;
import com.jbm.cluster.api.service.fegin.client.BaseAuthorityServiceClient;
import com.jbm.cluster.api.service.fegin.client.GatewayServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 动态资源加载器
 *
 * @author wesley.zhang
 */
@Slf4j
public class DynamicResourceLocator extends DynamicResourceService {


    @Autowired
    private BaseApiServiceClient baseApiServiceClient;

    @Autowired
    private BaseAppServiceClient baseAppServiceClient;
    /**
     * 权限资源
     */
    private List<AuthorityResource> authorityResources;

    /**
     * ip黑名单
     */
    private List<IpLimitApi> ipBlacks;

    /**
     * ip白名单
     */
    private List<IpLimitApi> ipWhites;

//    /**
//     * 权限列表
//     */
//    private Map<String, Collection<ConfigAttribute>> configAttributes = new ConcurrentHashMap<>();
    /**
     * 缓存
     */
    private Map<String, Object> cache = new ConcurrentHashMap<>();


    @Autowired
    private BaseAuthorityServiceClient baseAuthorityServiceClient;
    @Autowired
    private GatewayServiceClient gatewayServiceClient;

    private RouteDefinitionLocator routeDefinitionLocator;

    public DynamicResourceLocator() {
        authorityResources = new CopyOnWriteArrayList<>();
        ipBlacks = new CopyOnWriteArrayList<>();
        ipWhites = new CopyOnWriteArrayList<>();
    }


    public DynamicResourceLocator(RouteDefinitionLocator routeDefinitionLocator) {
        this();
        this.routeDefinitionLocator = routeDefinitionLocator;
    }

    /**
     * 清空缓存并刷新
     */
    public void refresh() {
//        this.configAttributes.clear();
        this.cache.clear();
        this.authorityResources = loadAuthorityResources();
        this.ipBlacks = loadIpBlackList();
        this.ipWhites = loadIpWhiteList();
    }

    /**
     * 获取路由后的地址
     *
     * @return
     */
    protected String getFullPath(String serviceId, String path) {
        final String[] fullPath = {path.startsWith("/") ? path : "/" + path};
        routeDefinitionLocator.getRouteDefinitions()
                .filter(routeDefinition -> routeDefinition.getId().equals(serviceId))
                .subscribe(routeDefinition -> {
                            routeDefinition.getPredicates().stream()
                                    .filter(predicateDefinition -> ("Path").equalsIgnoreCase(predicateDefinition.getName()))
                                    .filter(predicateDefinition -> !predicateDefinition.getArgs().containsKey("_rateLimit"))
                                    .forEach(predicateDefinition -> {
                                        fullPath[0] = predicateDefinition.getArgs().get("pattern").replace("/**", path.startsWith("/") ? path : "/" + path);
                                    });
                        }
                );
        return fullPath[0];
    }

    /**
     * 加载授权列表
     */
    public List<AuthorityResource> loadAuthorityResources() {
        List<AuthorityResource> resources = Lists.newArrayList();
//        Collection<ConfigAttribute> array;
//        ConfigAttribute cfg;
        try {
            // 查询所有接口
            resources = baseAuthorityServiceClient.findAuthorityResource().getResult();
            if (resources != null) {
                for (AuthorityResource item : resources) {
                    String path = item.getPath();
                    if (path == null) {
                        continue;
                    }
                    String fullPath = getFullPath(item.getServiceId(), path);
                    item.setPath(fullPath);
//                    array = configAttributes.get(fullPath);
//                    if (array == null) {
//                        array = new ArrayList<>();
//                    }
//                    if (!array.contains(item.getAuthority())) {
//                        cfg = new SecurityConfig(item.getAuthority());
//                        array.add(cfg);
//                    }
//                    configAttributes.put(fullPath, array);
                }
                log.info("=============加载动态权限:{}==============", resources.size());
            }
        } catch (Exception e) {
            log.error("加载动态权限错误:{}", e);
        }
        return resources;
    }

    /**
     * 加载IP黑名单
     */
    public List<IpLimitApi> loadIpBlackList() {
        List<IpLimitApi> list = Lists.newArrayList();
        try {
            list = gatewayServiceClient.getApiBlackList().getResult();
            if (list != null) {
                for (IpLimitApi item : list) {
                    item.setPath(getFullPath(item.getServiceId(), item.getPath()));
                }
                log.info("=============加载IP黑名单:{}==============", list.size());
            }
        } catch (Exception e) {
            log.error("加载IP黑名单错误:{}", e);
        }
        return list;
    }

    /**
     * 加载IP白名单
     */
    public List<IpLimitApi> loadIpWhiteList() {
        List<IpLimitApi> list = Lists.newArrayList();
        try {
            list = gatewayServiceClient.getApiWhiteList().getResult();
            if (list != null) {
                for (IpLimitApi item : list) {
                    item.setPath(getFullPath(item.getServiceId(), item.getPath()));
                }
                log.info("=============加载IP白名单:{}==============", list.size());
            }
        } catch (Exception e) {
            log.error("加载IP白名单错误:{}", e);
        }
        return list;
    }

//    public List<BaseApi> loadOperateList() {
//        List<BaseApi> list = Lists.newArrayList();
//        try {
//            list = baseApiServiceClient.findApiByPath(gatewayLogs.getServiceId(), realPath).getResult();
//            if (list != null) {
//                for (IpLimitApi item : list) {
//                    item.setPath(getFullPath(item.getServiceId(), item.getPath()));
//                }
//                log.info("=============加载IP白名单:{}==============", list.size());
//            }
//        } catch (Exception e) {
//            log.error("加载IP白名单错误:{}", e);
//        }
//        return list;
//    }


}
