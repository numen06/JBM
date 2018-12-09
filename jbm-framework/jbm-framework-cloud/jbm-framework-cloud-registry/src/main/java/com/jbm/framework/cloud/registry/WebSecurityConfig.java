package com.jbm.framework.cloud.registry;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * @author: create by wesley
 * @date:2018/12/9
 */
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * 支持url判断身份
     * @param http
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.NEVER);
        http.authorizeRequests().anyRequest().fullyAuthenticated();
        http.csrf().disable();
        http.authorizeRequests().anyRequest().authenticated().and().httpBasic();
    }

//    @Override
//    protected void configure(AuthenticationManagerBuilder auth)throws Exception{
//        auth.inMemoryAuthentication().withUser(admin_name).password(admin_password).roles(admin_roles)
//                .and().withUser(pc_name).password(pc_password).roles(pc_roles)//PC 服务
//                .and().withUser(app_name).password(app_password).roles(app_roles)//APP 服务
//                .and().withUser(zuul_name).password(zuul_password).roles(zuul_roles) //路由
//                .and().withUser(apiuser_name).password(apiuser_password).roles(apiuser_roles);//接口调用者
//    }

}
