package com.jbm.cluster.platform.gateway.filter;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.platform.gateway.config.properties.IgnoreWhiteProperties;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

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
                }).setError(e -> JSON.toJSONString(
                        ResultBody.failed().code(HttpStatus.UNAUTHORIZED.value()).msg("网关认证失败，无法访问系统资源")));
    }
}
