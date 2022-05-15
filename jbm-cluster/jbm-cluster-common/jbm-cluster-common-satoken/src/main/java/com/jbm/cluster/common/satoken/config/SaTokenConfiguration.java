package com.jbm.cluster.common.satoken.config;

import cn.dev33.satoken.jwt.StpLogicJwtForStyle;
import cn.dev33.satoken.stp.StpLogic;
import com.jbm.cluster.common.satoken.core.dao.RedisSaTokenDao;
import com.jbm.cluster.common.satoken.core.service.SaPermissionImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 配置
 *
 * @author Lion Li
 */
@Configuration
public class SaTokenConfiguration {

    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForStyle();
    }

    @Bean
    public RedisSaTokenDao redisSaTokenDao() {
        return new RedisSaTokenDao();
    }

    @Bean
    public SaPermissionImpl saPermissionImpl() {
        return new SaPermissionImpl();
    }


}
