package com.jbm.cluster.auth.config;

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.interceptor.SaAnnotationInterceptor;
import cn.dev33.satoken.interceptor.SaRouteInterceptor;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.satoken.config.SaUnknownRuntimeExceptionFilter;
import com.jbm.cluster.common.satoken.core.filter.SaServletSuperFilter;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import jbm.framework.boot.autoconfigure.filter.UnknownRuntimeExceptionFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Set;

/**
 * @Created wesley.zhang
 * @Date 2022/4/27 2:46
 * @Description TODO
 */
@Configuration
public class JbmAuthSecurityConfiguration implements WebMvcConfigurer {
    @Autowired
    private JbmClusterProperties jbmClusterProperties;

    private static final String[] excludeUrls = {"/actuator/**", "/v2/api-docs/**", "/login/**", "/token/**", "/oauth2/**"};

    /**
     * 注册sa-token的拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册路由拦截器，自定义验证规则
        // 这里只处理登录缓存清理 具体拦截再网关处理
        registry.addInterceptor(new SaRouteInterceptor((request, response, handler) -> {
            // 获取所有的
            SaRouter.match("/**");
        }) {
            @SuppressWarnings("all")
            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
                LoginHelper.clearCache();
            }
        }).addPathPatterns("/**");
        // 注解拦截器
        registry.addInterceptor(new SaAnnotationInterceptor()).addPathPatterns("/**");
    }

    /**
     * 校验是否从网关转发
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        Set<String> whiteList = new HashSet<>();
        CollUtil.addAll(whiteList, excludeUrls);
        CollUtil.addAll(whiteList, jbmClusterProperties.getPermitAll());
        return new SaServletSuperFilter()
                .addInclude("/**")
                .addExclude(ArrayUtil.toArray(whiteList, String.class))
                .setAuth(obj -> SaOAuth2Util.checkAccessToken(StpUtil.getTokenValue()));
//                .setAuth(obj -> SaIdUtil.checkCurrentRequestToken());
//                .setError(e -> ResultBody.failed().msg("服务认证失败，无法访问系统资源")
//                        .httpStatus(HttpStatus.UNAUTHORIZED.value()).code(HttpStatus.UNAUTHORIZED.value()));
    }



}
