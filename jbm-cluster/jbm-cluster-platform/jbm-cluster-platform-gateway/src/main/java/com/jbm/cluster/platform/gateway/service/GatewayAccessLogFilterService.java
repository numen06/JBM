package com.jbm.cluster.platform.gateway.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.jbm.cluster.api.model.gateway.GatewayLogInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gateway-side access log storage filter.
 *
 * <p>Rules here prevent collection before logs enter RabbitMQ. Python logs keeps
 * the same rules as a fallback for old gateway instances and direct ingest.</p>
 */
@Slf4j
@Service
public class GatewayAccessLogFilterService {

    public static final String NACOS_DATA_ID = "log-filter-rules.json";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final AtomicReference<List<AccessLogFilterRule>> rules = new AtomicReference<>(builtinRules());

    @Autowired(required = false)
    private ConfigService configService;

    @Autowired(required = false)
    private NacosConfigManager nacosConfigManager;

    @Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}")
    private String nacosGroup;

    @PostConstruct
    public void init() {
        ConfigService nacosConfigService = resolveConfigService();
        if (nacosConfigService == null) {
            log.info("Nacos ConfigService is unavailable; access log filter uses built-in rules only");
            return;
        }
        try {
            String content = nacosConfigService.getConfig(NACOS_DATA_ID, nacosGroup, 3000);
            reloadFromNacos(content);
            nacosConfigService.addListener(NACOS_DATA_ID, nacosGroup, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    reloadFromNacos(configInfo);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to initialize access log filter rules from Nacos", e);
        }
    }

    private ConfigService resolveConfigService() {
        if (configService != null) {
            return configService;
        }
        if (nacosConfigManager != null) {
            return nacosConfigManager.getConfigService();
        }
        return null;
    }

    public boolean shouldSkipBeforeCapture(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethodValue();
        for (AccessLogFilterRule rule : rules.get()) {
            if (StrUtil.isNotBlank(rule.getServiceId()) || StrUtil.isNotBlank(rule.getStatusCode())) {
                continue;
            }
            if (matches(rule, path, method, null, null)) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldSkip(GatewayLogInfo info) {
        if (info == null) {
            return false;
        }
        String status = info.getHttpStatus() == null ? null : String.valueOf(info.getHttpStatus());
        for (AccessLogFilterRule rule : rules.get()) {
            if (matches(rule, info.getPath(), info.getMethod(), info.getServiceId(), status)) {
                return true;
            }
        }
        return false;
    }

    private void reloadFromNacos(String content) {
        List<AccessLogFilterRule> next = new ArrayList<>(builtinRules());
        if (StrUtil.isNotBlank(content)) {
            try {
                JSONArray array;
                String trimmed = content.trim();
                if (trimmed.startsWith("[")) {
                    array = JSON.parseArray(trimmed);
                } else {
                    JSONObject object = JSON.parseObject(trimmed);
                    array = object.getJSONArray("rules");
                }
                if (array != null) {
                    for (int i = 0; i < array.size(); i++) {
                        JSONObject item = array.getJSONObject(i);
                        if (item == null || item.getBooleanValue("builtin")) {
                            continue;
                        }
                        AccessLogFilterRule rule = item.toJavaObject(AccessLogFilterRule.class);
                        if (rule.isEnabled() && rule.hasCondition()) {
                            next.add(rule);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse {} content; keep previous access log filter rules", NACOS_DATA_ID, e);
                return;
            }
        }
        rules.set(Collections.unmodifiableList(next));
        log.info("Loaded {} access log filter rules", next.size());
    }

    private boolean matches(AccessLogFilterRule rule, String path, String method, String serviceId, String statusCode) {
        if (!rule.isEnabled() || !rule.hasCondition()) {
            return false;
        }
        if (StrUtil.isNotBlank(rule.getPathPattern()) && !pathMatcher.match(rule.getPathPattern(), path)) {
            return false;
        }
        if (StrUtil.isNotBlank(rule.getMethod()) && !rule.getMethod().equalsIgnoreCase(method)) {
            return false;
        }
        if (StrUtil.isNotBlank(rule.getServiceId()) && !rule.getServiceId().equals(serviceId)) {
            return false;
        }
        if (StrUtil.isNotBlank(rule.getStatusCode()) && !rule.getStatusCode().equals(statusCode)) {
            return false;
        }
        return true;
    }

    private static List<AccessLogFilterRule> builtinRules() {
        List<AccessLogFilterRule> list = new ArrayList<>();
        list.add(rule("builtin-gateway-logs-root", "日志查询接口", "/GatewayLogs/**"));
        list.add(rule("builtin-gateway-logs-service", "日志查询接口", "/**/GatewayLogs/**"));
        list.add(rule("builtin-cluster-access-root", "访问统计接口", "/clusterAccess/**"));
        list.add(rule("builtin-cluster-access-service", "访问统计接口", "/**/clusterAccess/**"));
        list.add(rule("builtin-business-log-get-root", "业务日志读取接口", "/businessLog/get/**"));
        list.add(rule("builtin-business-log-get-service", "业务日志读取接口", "/**/businessLog/get/**"));
        list.add(rule("builtin-business-log-query-root", "业务日志查询接口", "/businessLog/query"));
        list.add(rule("builtin-business-log-query-service", "业务日志查询接口", "/**/businessLog/query"));
        list.add(rule("builtin-business-log-stream-root", "业务日志流接口", "/businessLog/stream/**"));
        list.add(rule("builtin-business-log-stream-service", "业务日志流接口", "/**/businessLog/stream/**"));
        list.add(rule("builtin-actuator", "健康检查", "/actuator/**"));
        list.add(rule("builtin-api-docs", "接口文档", "/**/v2/api-docs/**"));
        list.add(rule("builtin-webjars", "静态资源", "/webjars/**"));
        return Collections.unmodifiableList(list);
    }

    private static AccessLogFilterRule rule(String id, String name, String pathPattern) {
        AccessLogFilterRule rule = new AccessLogFilterRule();
        rule.setRuleId(id);
        rule.setRuleName(name);
        rule.setBuiltin(true);
        rule.setEnabled(true);
        rule.setPathPattern(pathPattern);
        return rule;
    }

    @Data
    public static class AccessLogFilterRule {
        private String ruleId;
        private String ruleName;
        private boolean enabled = true;
        private boolean builtin;
        private String pathPattern;
        private String method;
        private String serviceId;
        private String statusCode;
        private String remark;

        boolean hasCondition() {
            return StrUtil.isNotBlank(pathPattern)
                    || StrUtil.isNotBlank(method)
                    || StrUtil.isNotBlank(serviceId)
                    || StrUtil.isNotBlank(statusCode);
        }
    }
}
