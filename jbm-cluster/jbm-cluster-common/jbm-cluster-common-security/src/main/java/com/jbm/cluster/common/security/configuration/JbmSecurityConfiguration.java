package com.jbm.cluster.common.security.configuration;

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.id.SaIdUtil;
import com.jbm.cluster.common.basic.configuration.config.JbmApiScanProperties;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Created wesley.zhang
 * @Date 2022/4/27 2:46
 * @Description TODO
 */
@Configuration
@EnableConfigurationProperties({JbmClusterProperties.class, JbmApiScanProperties.class})
public class JbmSecurityConfiguration implements WebMvcConfigurer {

    @Autowired
    private JbmClusterProperties jbmClusterProperties;
    @Autowired
    private JbmApiScanProperties jbmApiScanProperties;
    /**
     * 不需要拦截地址
     */
    public static final String[] excludeUrls = {"/login", "/logout", "/refresh"};

    /**
     * 注册sa-token的拦截器
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
        return new SaServletFilter()
                .addInclude("/**")
                .addExclude("/actuator/**", "/v2/api-docs/**")
                .setAuth(obj -> SaIdUtil.checkCurrentRequestToken())
                .setError(e -> ResultBody.failed().msg("服务认证失败，无法访问系统资源").httpStatus(HttpStatus.UNAUTHORIZED.value()).code(HttpStatus.UNAUTHORIZED.value()));
    }

//    @Autowired
//    private BusProperties busProperties;

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder jbmClusterNotification() {
        return SecurityUtils.getPasswordEncoder();
    }


}
