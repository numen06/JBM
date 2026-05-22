package com.jbm.cluster.common.satoken.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenConfigTest {

    @Test
    void isConfigUnified_whenAllTimeoutsMatch() {
        TokenConfig config = new TokenConfig();
        ReflectionTestUtils.setField(config, "oauth2AccessTokenTimeout", 86400);
        ReflectionTestUtils.setField(config, "oauth2ClientTokenTimeout", 86400);
        ReflectionTestUtils.setField(config, "tokenTimeout", 86400);
        ReflectionTestUtils.setField(config, "clientTokenCacheHours", 24);
        assertTrue(config.isConfigUnified());
    }

    @Test
    void isConfigUnified_falseWhenAccessTokenDiffers() {
        TokenConfig config = new TokenConfig();
        ReflectionTestUtils.setField(config, "oauth2AccessTokenTimeout", 3600);
        ReflectionTestUtils.setField(config, "oauth2ClientTokenTimeout", 86400);
        ReflectionTestUtils.setField(config, "tokenTimeout", 86400);
        ReflectionTestUtils.setField(config, "clientTokenCacheHours", 24);
        assertFalse(config.isConfigUnified());
    }

    @Test
    void getUnifiedTokenTimeout_returnsMinimum() {
        TokenConfig config = new TokenConfig();
        ReflectionTestUtils.setField(config, "oauth2AccessTokenTimeout", 3600);
        ReflectionTestUtils.setField(config, "tokenTimeout", 86400);
        assertTrue(config.getUnifiedTokenTimeout() == 3600);
    }
}