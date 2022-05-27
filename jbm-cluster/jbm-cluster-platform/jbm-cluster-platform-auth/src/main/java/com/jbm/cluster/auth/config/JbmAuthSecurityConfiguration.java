package com.jbm.cluster.auth.config;

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.interceptor.SaAnnotationInterceptor;
import cn.dev33.satoken.interceptor.SaRouteInterceptor;
import cn.dev33.satoken.router.SaRouter;
import com.jbm.cluster.common.satoken.config.SaUnknownRuntimeExceptionFilter;
import com.jbm.cluster.common.satoken.core.filter.SaServletSuperFilter;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Created wesley.zhang
 * @Date 2022/4/27 2:46
 * @Description TODO
 */
@Configuration
public class JbmAuthSecurityConfiguration implements WebMvcConfigurer {

    /**
     * 不需要拦截地址
     */
    public static final String[] excludeUrls = {"/login", "/logout", "/refresh"};

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
        return new SaServletSuperFilter()
                .addInclude("/**")
                .addExclude("/actuator/**", "/v2/api-docs/**", "/login/**", "/token/**", "/oauth2/**")
                .setAuth(obj -> SaIdUtil.checkCurrentRequestToken());
//                .setError(e -> ResultBody.failed().msg("服务认证失败，无法访问系统资源")
//                        .httpStatus(HttpStatus.UNAUTHORIZED.value()).code(HttpStatus.UNAUTHORIZED.value()));
    }


    @Bean
    public SaUnknownRuntimeExceptionFilter saUnknownRuntimeExceptionFilter() {
        return new SaUnknownRuntimeExceptionFilter();
    }


}
