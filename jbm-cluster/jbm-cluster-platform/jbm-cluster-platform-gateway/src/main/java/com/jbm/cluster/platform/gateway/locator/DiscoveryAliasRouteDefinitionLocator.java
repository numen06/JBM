package com.jbm.cluster.platform.gateway.locator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.support.NameUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds /{serviceAlias}/** routes from Nacos discovery without DB rows.
 */
public class DiscoveryAliasRouteDefinitionLocator implements RouteDefinitionLocator {

    private static final String PLATFORM_PREFIX = "jbm-cluster-platform-";
    private static final Map<String, String> DEFAULT_SERVICE_ALIASES = defaultServiceAliases();

    private final ReactiveDiscoveryClient discoveryClient;
    private final String profileName;

    public DiscoveryAliasRouteDefinitionLocator(ReactiveDiscoveryClient discoveryClient, String profileName) {
        this.discoveryClient = discoveryClient;
        this.profileName = profileName;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return discoveryClient.getServices()
                .filter(StrUtil::isNotBlank)
                .filter(serviceId -> StrUtil.startWith(serviceId, PLATFORM_PREFIX))
                .flatMapIterable(this::buildRouteDefinitions);
    }

    private List<RouteDefinition> buildRouteDefinitions(String serviceId) {
        Set<String> aliases = aliasesFor(serviceId);
        if (CollUtil.isEmpty(aliases)) {
            return Collections.emptyList();
        }
        List<RouteDefinition> definitions = new ArrayList<>(aliases.size());
        for (String alias : aliases) {
            if (isPushService(serviceId)) {
                definitions.add(buildRouteDefinition(serviceId, "/" + alias + "/ws/**", 1, alias + "-ws", true));
            }
            definitions.add(buildRouteDefinition(serviceId, "/" + alias + "/**", 1, alias));
        }
        return definitions;
    }

    private RouteDefinition buildRouteDefinition(String serviceId, String path, int stripPrefixCount, String routeKey) {
        return buildRouteDefinition(serviceId, path, stripPrefixCount, routeKey, false);
    }

    private RouteDefinition buildRouteDefinition(String serviceId, String path, int stripPrefixCount, String routeKey, boolean websocket) {
        RouteDefinition definition = new RouteDefinition();
        definition.setId("discovery-alias-" + serviceId + "-" + routeKey);
        definition.setUri(websocket ? websocketServiceUri(serviceId) : serviceUri(serviceId));
        definition.setOrder(websocket ? 50 : 100);

        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        Map<String, String> predicateArgs = new HashMap<>(4);
        predicateArgs.put("pattern", path);
        predicateArgs.put("pathPattern", path);
        predicate.setArgs(predicateArgs);
        definition.setPredicates(Collections.singletonList(predicate));

        if (stripPrefixCount > 0) {
            FilterDefinition stripPrefix = new FilterDefinition();
            stripPrefix.setName("StripPrefix");
            Map<String, String> filterArgs = new HashMap<>(2);
            filterArgs.put(NameUtils.GENERATED_NAME_PREFIX + "0", String.valueOf(stripPrefixCount));
            stripPrefix.setArgs(filterArgs);
            definition.setFilters(Collections.singletonList(stripPrefix));
        }
        return definition;
    }

    private URI serviceUri(String serviceId) {
        return UriComponentsBuilder.fromUriString("lb://" + serviceId).build().toUri();
    }

    private URI websocketServiceUri(String serviceId) {
        return UriComponentsBuilder.fromUriString("lb:ws://" + serviceId).build().toUri();
    }

    private Set<String> aliasesFor(String serviceId) {
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(serviceId);
        String shortName = StrUtil.removePrefix(serviceId, PLATFORM_PREFIX);
        aliases.add(shortName);
        addDefaultAlias(aliases, serviceId);
        String profileSuffix = "-" + StrUtil.blankToDefault(profileName, "");
        if (StrUtil.isNotBlank(profileName) && StrUtil.endWith(shortName, profileSuffix)) {
            String profileFreeShortName = StrUtil.removeSuffix(shortName, profileSuffix);
            aliases.add(profileFreeShortName);
            addDefaultAlias(aliases, PLATFORM_PREFIX + profileFreeShortName);
        }
        int lastDash = shortName.lastIndexOf('-');
        if (lastDash > 0) {
            aliases.add(shortName.substring(0, lastDash));
        }
        return aliases;
    }

    private void addDefaultAlias(Set<String> aliases, String serviceId) {
        String alias = DEFAULT_SERVICE_ALIASES.get(serviceId);
        if (StrUtil.isNotBlank(alias)) {
            aliases.add(alias);
        }
    }

    private boolean isPushService(String serviceId) {
        return StrUtil.equals(serviceId, JbmClusterConstants.PUSH_SERVER)
                || StrUtil.startWith(serviceId, JbmClusterConstants.PUSH_SERVER + "-");
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
