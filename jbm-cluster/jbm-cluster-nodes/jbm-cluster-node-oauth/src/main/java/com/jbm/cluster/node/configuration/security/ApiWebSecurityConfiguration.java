package com.jbm.cluster.node.configuration.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * 多个WebSecurityConfigurerAdapter
 */
@ConditionalOnClass(WebSecurityConfigurerAdapter.class)
@Order(101)
public class ApiWebSecurityConfiguration extends WebSecurityConfigurerAdapter {
    public static final String[] AUTH_WHITELIST = {
            "/error",
            "/static/**",
            "/v2/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/favicon.ico"
    };

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers(AUTH_WHITELIST).permitAll()
                .anyRequest().authenticated();
    }
}
