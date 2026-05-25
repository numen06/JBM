package com.jbm.cluster.platform.gateway.filter;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.platform.gateway.config.properties.IgnoreWhiteProperties;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

import java.util.HashSet;
import java.util.Set;

/**
 * 网关层 Sa-Token 全局过滤器（WebFlux）。
 * <p>
 * <b>设计说明</b>：网关不在此层执行 {@code StpUtil.checkLogin()}。登录/OAuth2 校验由下游
 * Center/Auth 的 {@link com.jbm.cluster.common.security.configuration.JbmSecurityConfiguration} 负责；
 * 网关侧仅做白名单排除、API 签名（{@link ApiSignatureFilter}）、API Key 授权
 * （{@link DeveloperAuthFilter}）及内部互信 Header 注入（{@link ForwardAuthFilter}）。
 * 白名单路径见 {@code security.ignore.whites} 与 {@code jbm.api.sign-ignores}。
 * </p>
 *
 * @author Lion Li
 */
@Configuration
public class SaAuthFilter {

    /**
     * 硬编码补充放行（与 security.ignore.whites 中 favicon/actuator 对齐，避免配置未加载时遗漏）
     */
    private static final String[] excludeUrls = {"/favicon.ico", "/static/favicon.ico", "/actuator/**"};

    @Bean
    public SaReactorFilter getSaReactorFilter(IgnoreWhiteProperties ignoreWhite) {
        Set<String> whiteList = new HashSet<>();
        CollUtil.addAll(whiteList, excludeUrls);
        CollUtil.addAll(whiteList, ignoreWhite.getWhites());
        return new SaReactorFilter()
                .addInclude("/**")
                .addExclude(ArrayUtil.toArray(whiteList, String.class))
                //  intentionally 不调用 StpUtil.checkLogin()，见类注释
                .setAuth(obj -> SaRouter.match("/**", r -> {
                })).setError(e -> {
                    HttpStatus status = HttpStatus.UNAUTHORIZED;
                    String msg = "网关认证失败，无法访问系统资源";
                    if (e instanceof NotPermissionException) {
                        status = HttpStatus.FORBIDDEN;
                        msg = StrUtil.blankToDefault(e.getMessage(), "无权限访问");
                    } else if (e instanceof NotLoginException) {
                        msg = StrUtil.blankToDefault(e.getMessage(), msg);
                    } else if (StrUtil.isNotBlank(e.getMessage())) {
                        msg = e.getMessage();
                    }
                    ServerWebExchange ctx = SaReactorSyncHolder.getContext();
                    if (ctx != null) {
                        ctx.getResponse().setStatusCode(status);
                    }
                    return JSON.toJSONString(ResultBody.failed()
                            .code(status.value())
                            .msg(msg)
                            .httpStatus(status.value()));
                });
    }
}
