package com.jbm.cluster.common.security.configuration;

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaAnnotationInterceptor;
import cn.dev33.satoken.interceptor.SaRouteInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.common.basic.configuration.apis.ApiBuild;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.satoken.core.filter.SaOAuthFilterAuthStrategy;
import com.jbm.cluster.common.satoken.core.filter.SaServletSuperFilter;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.cluster.common.security.annotation.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
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
    public SaServletFilter getSaServletFilter(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        Set<String> whiteList = new HashSet<>();
        CollUtil.addAll(whiteList, excludeUrls);
        CollUtil.addAll(whiteList, jbmClusterProperties.getPermitAll());
        CollUtil.addAll(whiteList, this.getPermitAllAnn(requestMappingHandlerMapping));
        return new SaServletSuperFilter()
                .addInclude("/**")
                .addExclude(ArrayUtil.toArray(whiteList, String.class))
                .setAuth(new SaOAuthFilterAuthStrategy());
//                .setAuth(obj -> SaIdUtil.checkCurrentRequestToken());
    }

    private Set<String> getPermitAllAnn(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        Set<String> strSet = new HashSet<>();
        try {
            // 获取url与类和方法的对应信息
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
            handlerMethods.entrySet().parallelStream().forEach(handlerMethodEntry -> {
                // 判断方法是否有@PermitAll注解
                if (handlerMethodEntry.getValue().getMethodAnnotation(PermitAll.class) != null) {
                    String url = StrUtil.join(StrUtil.COMMA, handlerMethodEntry.getKey().getPatternsCondition().getPatterns());
                    // 将url添加到结果集合中
                    strSet.add(url);
                }
            });
        } catch (Exception e) {
            // 异常处理
            log.error("获取接口信息失败", e);
        }
        // 返回结果集合
        return strSet;
    }


//    @Autowired
//    private BusProperties busProperties;

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder jbmClusterNotification() {
        return SecurityUtils.getPasswordEncoder();
    }


}
