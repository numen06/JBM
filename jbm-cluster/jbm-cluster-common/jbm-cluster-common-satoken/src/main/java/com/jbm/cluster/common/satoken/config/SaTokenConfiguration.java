package com.jbm.cluster.common.satoken.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import com.jbm.cluster.common.satoken.core.StpLogicJwtForCustom;
import com.jbm.cluster.common.satoken.core.dao.RedisSaTokenDao;
import com.jbm.cluster.common.satoken.core.service.SaPermissionImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 基础配置（JWT、Redis、权限接口）。OAuth2 见 {@link SaOAuth2AutoConfiguration}。
 *
 * @author wesley
 */
@Configuration
public class SaTokenConfiguration {

    @Bean
    public StpLogicJwtForSimple getStpLogicJwt() {
        return new StpLogicJwtForCustom();
    }

    @Bean
    public RedisSaTokenDao redisSaTokenDao(SaTokenConfig saTokenConfig) {
        SaManager.setConfig(saTokenConfig);
        return new RedisSaTokenDao();
    }

    @Bean
    public SaPermissionImpl saPermissionImpl() {
        return new SaPermissionImpl();
    }
}
