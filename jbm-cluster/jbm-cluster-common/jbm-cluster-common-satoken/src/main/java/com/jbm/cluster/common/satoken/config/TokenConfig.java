package com.jbm.cluster.common.satoken.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Token过期时间配置管理类
 * 统一管理所有token类型的过期时间配置
 * 
 * @author Wesley.Zhang
 * @Date 2025-12-25
 */
@Component
public class TokenConfig {

    // ==================== OAuth2 Token 配置 ====================
    
    /**
     * OAuth2 Access Token 总有效期（秒）
     * 默认：24小时 = 86400秒
     */
    @Value("${sa-token.oauth2.access-token-timeout:86400}")
    private int oauth2AccessTokenTimeout;

    /**
     * OAuth2 Client Token 总有效期（秒）
     * 默认：24小时 = 86400秒
     */
    @Value("${sa-token.oauth2.client-token-timeout:86400}")
    private int oauth2ClientTokenTimeout;

    // ==================== 通用 Token 配置 ====================
    
    /**
     * 通用Token总有效期（秒）
     * 默认：24小时 = 86400秒
     */
    @Value("${sa-token.timeout:86400}")
    private int tokenTimeout;

    /**
     * Token活动超时时间（秒）- 无操作过期时间
     * 默认：2小时 = 7200秒
     */
    @Value("${sa-token.activity-timeout:7200}")
    private int tokenActivityTimeout;

    // ==================== Id-Token 配置 ====================
    
    /**
     * Id-Token 总有效期（秒）
     * 默认：7天 = 604800秒
     */
    @Value("${sa-token.id-token-timeout:604800}")
    private int idTokenTimeout;

    // ==================== Client Token 缓存配置 ====================
    
    /**
     * Client Token 缓存过期时间（小时）
     */
    @Value("${sa-token.oauth2.client-token-cache-hours:24}")
    private int clientTokenCacheHours;

    // ==================== Getter 方法 ====================

    public int getOauth2AccessTokenTimeout() {
        return oauth2AccessTokenTimeout;
    }

    public int getOauth2ClientTokenTimeout() {
        return oauth2ClientTokenTimeout;
    }

    public int getTokenTimeout() {
        return tokenTimeout;
    }

    public int getTokenActivityTimeout() {
        return tokenActivityTimeout;
    }

    public int getIdTokenTimeout() {
        return idTokenTimeout;
    }

    public int getClientTokenCacheHours() {
        return clientTokenCacheHours;
    }

    // ==================== 配置验证方法 ====================

    /**
     * 验证SA/OAuth配置是否统一
     * 确保OAuth2和通用token使用相同的过期时间
     * 注意：Id-Token独立配置，不参与此检查
     */
    public boolean isConfigUnified() {
        return oauth2AccessTokenTimeout == tokenTimeout 
            && oauth2ClientTokenTimeout == tokenTimeout
            && clientTokenCacheHours == 24;
    }

    /**
     * 获取统一的token总有效期（秒）
     */
    public int getUnifiedTokenTimeout() {
        return Math.min(oauth2AccessTokenTimeout, tokenTimeout);
    }

    /**
     * 获取统一的活动超时时间（秒）
     */
    public int getUnifiedActivityTimeout() {
        return tokenActivityTimeout;
    }
}
