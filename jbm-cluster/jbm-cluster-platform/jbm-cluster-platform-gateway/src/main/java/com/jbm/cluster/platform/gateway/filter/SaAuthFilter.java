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
 * [Sa-Token 权限认证] 拦截器
 *
 * @author Lion Li
 */
@Configuration
public class SaAuthFilter {

    /**
     * 不需要拦截地址
     */
    private static final String[] excludeUrls = {"/favicon.ico", "/static/favicon.ico", "/actuator/**"};

    // 注册 Sa-Token全局过滤器
    @Bean
    public SaReactorFilter getSaReactorFilter(IgnoreWhiteProperties ignoreWhite) {
        Set<String> whiteList = new HashSet<>();
        CollUtil.addAll(whiteList, excludeUrls);
        CollUtil.addAll(whiteList, ignoreWhite.getWhites());
        return new SaReactorFilter()
                // 拦截地址
                .addInclude("/**")
                // 开放地址
                .addExclude(ArrayUtil.toArray(whiteList, String.class))
                // 鉴权方法：每次访问进入
                .setAuth(obj -> {
                    // 登录校验 -- 拦截所有路由
                    SaRouter.match("/**", r -> {
                        // 检查是否登录 是否有token
//                        StpUtil.checkLogin();
                        // 有效率影响 用于临时测试
                        // if (log.isDebugEnabled()) {
                        //     log.debug("剩余有效时间: {}", StpUtil.getTokenTimeout());
                        //     log.debug("临时有效时间: {}", StpUtil.getTokenActivityTimeout());
                        // }
                    });
                }).setError(e -> {
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
