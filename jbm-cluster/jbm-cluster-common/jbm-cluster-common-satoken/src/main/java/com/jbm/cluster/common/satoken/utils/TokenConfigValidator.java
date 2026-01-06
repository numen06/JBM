package com.jbm.cluster.common.satoken.utils;

import com.jbm.cluster.common.satoken.config.TokenConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Token配置验证器
 * 验证所有token过期时间配置的一致性
 * 
 * @author Wesley.Zhang
 * @Date 2025-12-25
 */
@Slf4j
@Component
public class TokenConfigValidator {

    @Autowired
    private TokenConfig tokenConfig;

    @PostConstruct
    public void validate() {
        log.info("========== Token配置验证开始 ==========");
        
        // 验证OAuth2配置
        log.info("OAuth2 Access Token 超时时间: {}秒 ({}小时)", 
                tokenConfig.getOauth2AccessTokenTimeout(), 
                tokenConfig.getOauth2AccessTokenTimeout() / 3600);
        log.info("OAuth2 Client Token 超时时间: {}秒 ({}小时)", 
                tokenConfig.getOauth2ClientTokenTimeout(), 
                tokenConfig.getOauth2ClientTokenTimeout() / 3600);
        
        // 验证通用Token配置
        log.info("通用 Token 超时时间: {}秒 ({}小时)", 
                tokenConfig.getTokenTimeout(), 
                tokenConfig.getTokenTimeout() / 3600);
        log.info("Token 活动超时时间: {}秒 ({}小时)", 
                tokenConfig.getTokenActivityTimeout(), 
                tokenConfig.getTokenActivityTimeout() / 3600);
        
        // 验证Id-Token配置
        log.info("Id-Token 超时时间: {}秒 ({}天)", 
                tokenConfig.getIdTokenTimeout(), 
                tokenConfig.getIdTokenTimeout() / 86400);
        
        // 验证Client Token缓存配置
        log.info("Client Token 缓存时间: {}小时", 
                tokenConfig.getClientTokenCacheHours());
        
        // 验证一致性 (Id-Token除外，它独立配置)
        boolean unified = tokenConfig.isConfigUnified();
        log.info("SA/OAuth Token统一性检查: {}", unified ? "✅ 通过" : "❌ 失败");
        
        if (!unified) {
            log.warn("⚠️  SA/OAuth Token配置不统一，建议调整:");
            if (tokenConfig.getOauth2AccessTokenTimeout() != tokenConfig.getTokenTimeout()) {
                log.warn("  - OAuth2 Access Token ({}) 与通用 Token ({}) 不一致", 
                        tokenConfig.getOauth2AccessTokenTimeout(), tokenConfig.getTokenTimeout());
            }
            if (tokenConfig.getOauth2ClientTokenTimeout() != tokenConfig.getTokenTimeout()) {
                log.warn("  - OAuth2 Client Token ({}) 与通用 Token ({}) 不一致", 
                        tokenConfig.getOauth2ClientTokenTimeout(), tokenConfig.getTokenTimeout());
            }
            if (tokenConfig.getClientTokenCacheHours() != 24) {
                log.warn("  - Client Token 缓存时间 ({}) 应为 24 小时", 
                        tokenConfig.getClientTokenCacheHours());
            }
        }
        
        log.info("Id-Token 独立配置: {}天 (不参与统一过期策略)", 
                tokenConfig.getIdTokenTimeout() / 86400);
        
        // 验证活动超时是否合理
        if (tokenConfig.getTokenActivityTimeout() >= tokenConfig.getTokenTimeout()) {
            log.error("❌ 配置错误: 活动超时时间不能大于总有效期");
        } else {
            log.info("✅ 活动超时配置合理");
        }
        
        log.info("========== Token配置验证结束 ==========");
    }
}
