package com.jbm.cluster.node.configuration;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import com.google.common.base.Charsets;
import com.jbm.cluster.common.configuration.JbmClusterProperties;
import com.jbm.cluster.common.exception.JbmAccessDeniedHandler;
import com.jbm.cluster.common.exception.JbmAuthenticationEntryPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-18 09:18
 **/
@Slf4j
@Configuration
//检查是否存在资源服务器配置
@ConditionalOnMissingBean(ResourceServerConfigurerAdapter.class)
//在资源构建器之后执行
//@AutoConfigureAfter({JwtTokenConfiguration.class, RedisTokenConfiguration.class, RemoteTokenConfiguration.class})
@EnableResourceServer
@EnableConfigurationProperties({JbmClusterProperties.class})
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

    /**
     * 找到token的构建类
     */
    @Autowired
    private ResourceServerTokenServices resourceServerTokenServices;

    @Autowired
    private JbmClusterProperties jbmClusterProperties;

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources.tokenServices(resourceServerTokenServices);
        initJbmNode();
    }

    public static void initJbmNode() {
        try {
            final String branner = FileUtil.readString("classpath:jbm.banner", Charsets.UTF_8);
            System.out.println(branner);
            log.info("JBM CLUSTER NODE SCUESS");
        } catch (Exception e) {

        }
    }


    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .and()
                .authorizeRequests()
                // 注意：根据业务需求,指定接口访问权限, fegin方式调用的接口,可以直接放行. 考虑到通过网关也可以直接访问,在接口管理中设置“禁止公开访问”即可
                .antMatchers(jbmClusterProperties.getPermitAll()).permitAll()
                // 指定监控访问权限
                .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
                .anyRequest().authenticated()
                .and()
                //认证鉴权错误处理,为了统一异常处理。每个资源服务器都应该加上。
                .exceptionHandling()
                .accessDeniedHandler(new JbmAccessDeniedHandler())
                .authenticationEntryPoint(new JbmAuthenticationEntryPoint())
                .and()
                .csrf().disable();
    }

}
