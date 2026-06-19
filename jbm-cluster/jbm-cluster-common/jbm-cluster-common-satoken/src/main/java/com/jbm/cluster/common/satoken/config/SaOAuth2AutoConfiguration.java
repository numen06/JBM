package com.jbm.cluster.common.satoken.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.oauth2.SaOAuth2Manager;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import com.jbm.cluster.common.satoken.core.dao.RedisSaTokenDao;
import com.jbm.cluster.common.satoken.oauth.JbmNodeOAuth2TemplateImpl;
import com.jbm.cluster.common.satoken.oauth.NodeClientModelSource;
import com.jbm.cluster.common.satoken.oauth.OAuth2AccessTokenExpirySyncListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@ConditionalOnClass(SaOAuth2Template.class)
public class SaOAuth2AutoConfiguration {

    @Autowired(required = false)
    private TokenConfig tokenConfig;

    @PostConstruct
    public void syncOAuth2TimeoutWithSaToken() {
        long timeout = SaManager.getConfig().getTimeout();
        if (tokenConfig != null) {
            timeout = tokenConfig.getUnifiedTokenTimeout();
        }
        SaOAuth2Manager.getConfig().setAccessTokenTimeout(timeout);
        SaOAuth2Manager.getConfig().setClientTokenTimeout(timeout);
        log.info("OAuth2 access/client token timeout aligned with Sa-Token: {}s", timeout);
    }

    @Bean
    @ConditionalOnBean(RedisSaTokenDao.class)
    @ConditionalOnExpression("'${jbm.security.auth.mode:mixed}' != 'oauth'")
    public OAuth2AccessTokenExpirySyncListener oauth2AccessTokenExpirySyncListener() {
        return new OAuth2AccessTokenExpirySyncListener();
    }

    @Bean
    @Primary
    @ConditionalOnBean(RedisSaTokenDao.class)
    @ConditionalOnExpression("'${jbm.security.auth.mode:mixed}' != 'oauth'")
    public SaOAuth2Template jbmNodeOAuth2Template(RedisSaTokenDao redisSaTokenDao) {
        SaManager.setSaTokenDao(redisSaTokenDao);
        return new JbmNodeOAuth2TemplateImpl();
    }

    @Bean
    public NodeClientModelSource nodeClientModelSource() {
        return new NodeClientModelSource();
    }
}
