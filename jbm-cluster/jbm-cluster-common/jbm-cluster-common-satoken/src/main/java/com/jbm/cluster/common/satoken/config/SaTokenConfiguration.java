package com.jbm.cluster.common.satoken.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.stp.StpLogic;
import com.jbm.cluster.common.satoken.core.StpLogicJwtForCustom;
import com.jbm.cluster.common.satoken.core.dao.RedisSaTokenDao;
import com.jbm.cluster.common.satoken.core.service.SaPermissionImpl;
import com.jbm.cluster.common.satoken.standardjwt.StandardJwtIssuer;
import com.jbm.cluster.common.satoken.standardjwt.StandardJwtIssuerProperties;
import com.jbm.cluster.common.satoken.standardjwt.StandardJwtLoginConverter;
import com.jbm.cluster.common.satoken.standardjwt.StandardJwtProperties;
import com.jbm.cluster.common.satoken.standardjwt.StandardJwtVerifier;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 基础配置（JWT、Redis、权限接口）。OAuth2 见 {@link SaOAuth2AutoConfiguration}。
 *
 * @author wesley
 */
@Configuration
@EnableConfigurationProperties({JbmAuthProperties.class, StandardJwtProperties.class, StandardJwtIssuerProperties.class})
public class SaTokenConfiguration {

    @Bean
    public StpLogic getStpLogicJwt(JbmAuthProperties authProperties) {
        return new StpLogicJwtForCustom(authProperties);
    }

    @Bean
    @ConditionalOnBean(RedisService.class)
    @ConditionalOnExpression("'${jbm.security.auth.mode:mixed}' != 'oauth'")
    public RedisSaTokenDao redisSaTokenDao(SaTokenConfig saTokenConfig) {
        SaManager.setConfig(saTokenConfig);
        return new RedisSaTokenDao();
    }

    @Bean
    public SaPermissionImpl saPermissionImpl() {
        return new SaPermissionImpl();
    }

    @Bean
    public StandardJwtLoginConverter standardJwtLoginConverter(StandardJwtProperties properties) {
        return new StandardJwtLoginConverter(properties);
    }

    @Bean
    public StandardJwtVerifier standardJwtVerifier(StandardJwtProperties properties,
                                                   StandardJwtLoginConverter loginConverter) {
        return new StandardJwtVerifier(properties, loginConverter);
    }

    @Bean
    public StandardJwtIssuer standardJwtIssuer(StandardJwtIssuerProperties properties) {
        return new StandardJwtIssuer(properties);
    }
}
