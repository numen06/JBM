package com.jbm.framework.cloud.auth.config;

import javax.servlet.http.HttpServletResponse;

import com.jbm.framework.cloud.auth.component.mobile.MobileSecurityConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

@Configuration
@EnableResourceServer
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

    @Autowired
    private MobileSecurityConfigurer mobileSecurityConfigurer;

    @Override
    public void configure(HttpSecurity http) throws Exception {
//		http.csrf().disable().exceptionHandling()
//				.authenticationEntryPoint(
//						(request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
//				.and().authorizeRequests().anyRequest().authenticated().and().httpBasic();
//
        http.apply(mobileSecurityConfigurer);
        http.csrf().disable().exceptionHandling()
                // 定义的不存在access_token时候响应
                .authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .and().authorizeRequests().antMatchers("/**/**").permitAll().anyRequest().authenticated().and()
                .httpBasic().disable();
    }
}