package com.jbm.cluster.common.satoken.utils;

import com.jbm.cluster.common.satoken.config.TokenConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Token配置验证器：启动时校验 SA/OAuth 过期配置一致，避免双层过期漂移。
 */
@Slf4j
@Component
public class TokenConfigValidator {

    @Autowired
    private TokenConfig tokenConfig;

    @PostConstruct
    public void validate() {
        log.info("========== Token配置验证开始 ==========");

        log.info("OAuth2 Access Token 超时时间: {}秒 ({}小时)",
                tokenConfig.getOauth2AccessTokenTimeout(),
                tokenConfig.getOauth2AccessTokenTimeout() / 3600);
        log.info("OAuth2 Client Token 超时时间: {}秒 ({}小时)",
                tokenConfig.getOauth2ClientTokenTimeout(),
                tokenConfig.getOauth2ClientTokenTimeout() / 3600);
        log.info("通用 Token 超时时间: {}秒 ({}小时)",
                tokenConfig.getTokenTimeout(),
                tokenConfig.getTokenTimeout() / 3600);
        log.info("Token 活动超时时间: {}秒 ({}小时)",
                tokenConfig.getTokenActivityTimeout(),
                tokenConfig.getTokenActivityTimeout() / 3600);
        log.info("Id-Token 超时时间: {}秒 ({}天)",
                tokenConfig.getIdTokenTimeout(),
                tokenConfig.getIdTokenTimeout() / 86400);

        boolean unified = tokenConfig.isConfigUnified();
        log.info("SA/OAuth Token统一性检查: {}", unified ? "通过" : "失败");

        if (!unified) {
            String msg = String.format(
                    "SA/OAuth Token 配置不一致: oauth2.access-token=%ds, oauth2.client-token=%ds, sa-token.timeout=%ds。"
                            + "请保持三者相同（见 sa-token.properties / bootstrap.yml）。",
                    tokenConfig.getOauth2AccessTokenTimeout(),
                    tokenConfig.getOauth2ClientTokenTimeout(),
                    tokenConfig.getTokenTimeout());
            throw new IllegalStateException(msg);
        }

        if (tokenConfig.getTokenActivityTimeout() >= tokenConfig.getTokenTimeout()) {
            throw new IllegalStateException("配置错误: sa-token.activity-timeout 不能大于或等于 sa-token.timeout");
        }

        log.info("OAuth2 AccessToken TTL will be aligned with Sa-Token Redis TTL at runtime");
        log.info("========== Token配置验证结束 ==========");
    }
}
