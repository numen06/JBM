package com.jbm.cluster.common.security.configuration;

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.interceptor.SaAnnotationInterceptor;
import cn.dev33.satoken.interceptor.SaRouteInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.satoken.core.filter.SaOAuthFilterAuthStrategy;
import com.jbm.cluster.common.satoken.core.filter.SaServletSuperFilter;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.cluster.common.security.annotation.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Created wesley.zhang
 * @Date 2022/4/27 2:46
 * @Description TODO
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({JbmClusterProperties.class})
public class JbmSecurityConfiguration implements WebMvcConfigurer {

    /**
     * 不需要拦截地址
     */
    public static final String[] excludeUrls = {"/actuator/**", "/v2/api-docs/**"};
    @Autowired
    private JbmClusterProperties jbmClusterProperties;

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
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                // 在请求开始时初始化ThreadLocal缓存，避免租户拦截器等场景获取不到用户信息
                LoginHelper.initCache();
                return super.preHandle(request, response, handler);
            }

            @SuppressWarnings("all")
            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
                // 请求结束后清除ThreadLocal缓存，防止内存泄漏
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
    public SaServletFilter getSaServletFilter(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        Set<String> whiteList = new HashSet<>();
        CollUtil.addAll(whiteList, excludeUrls);
        CollUtil.addAll(whiteList, jbmClusterProperties.getPermitAll());
        CollUtil.addAll(whiteList, this.getPermitAllAnn(requestMappingHandlerMapping));
        return new SaServletSuperFilter()
                .addInclude("/**")
                .addExclude(ArrayUtil.toArray(whiteList, String.class))
                .setAuth(obj -> {
                    HttpServletRequest request = (HttpServletRequest) cn.dev33.satoken.context.SaHolder.getRequest().getSource();
                    if (isGatewayTrustedRequest(request)) {
                        return;
                    }
                    new SaOAuthFilterAuthStrategy().run(obj);
                });
//                .setAuth(obj -> SaIdUtil.checkCurrentRequestToken());
    }

    private Set<String> getPermitAllAnn(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        Set<String> strSet = new HashSet<>();
        try {
            // 获取url与类和方法的对应信息
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
            for (Map.Entry<RequestMappingInfo, HandlerMethod> handlerMethodEntry : handlerMethods.entrySet()) {
                if (handlerMethodEntry.getValue().getMethodAnnotation(PermitAll.class) != null) {
                    String url = StrUtil.join(StrUtil.COMMA, handlerMethodEntry.getKey().getPatternsCondition().getPatterns());
                    strSet.add(url);
                }
            }
        } catch (Exception e) {
            // 异常处理
            log.error("获取接口信息失败", e);
        }
        // 返回结果集合
        return strSet;
    }

    private static boolean isGatewayTrustedRequest(HttpServletRequest request) {
        String authorization = request.getHeader(JbmSecurityConstants.AUTHORIZATION_HEADER);
        if (StrUtil.isNotBlank(authorization)) {
            return false;
        }
        String idToken = request.getHeader(SaIdUtil.ID_TOKEN);
        if (StrUtil.isNotBlank(idToken) && SaIdUtil.isValid(idToken)) {
            return true;
        }
        return StrUtil.isNotBlank(request.getHeader(JbmSecurityConstants.GATEWAY_API_KEY_ID))
                && StrUtil.isNotBlank(request.getHeader(JbmSecurityConstants.INTERNAL_SERVICE));
    }


//    @Autowired
//    private BusProperties busProperties;

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder jbmClusterNotification() {
        return SecurityUtils.getPasswordEncoder();
    }


}
