package com.jbm.cluster.common.mysql.service.openapi;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class OpenApiRouteAliasSupport {

    private static final String PLATFORM_PREFIX = "jbm-cluster-platform-";
    private static final Map<String, String> DEFAULT_SERVICE_ALIASES = defaultServiceAliases();

    private final Environment environment;

    public OpenApiRouteAliasSupport(Environment environment) {
        this.environment = environment;
    }

    public String routeAliasFor(String serviceId) {
        if (StrUtil.isBlank(serviceId)) {
            return serviceId;
        }
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
