package com.jbm.cluster.common.basic.service;

import com.jbm.cluster.common.basic.configuration.config.JbmDebugProperties;
import com.jbm.cluster.core.constant.JbmConstants;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 系统调试模式（环境变量 / Redis 全局开关，默认关闭）
 */
public class SysDebugModeService {

    @Autowired
    private RedisService redisService;
    @Autowired
    private JbmDebugProperties jbmDebugProperties;

    public boolean isEnvDebugModeEnabled() {
        return Boolean.TRUE.equals(jbmDebugProperties.getDebug());
    }

    public boolean isRedisDebugModeEnabled() {
        Boolean val = redisService.getCacheObject(JbmConstants.SYS_CONFIG_DEBUG_MODE_KEY);
        return Boolean.TRUE.equals(val);
    }

    public boolean isDebugModeEnabled() {
        return isEnvDebugModeEnabled() || isRedisDebugModeEnabled();
    }

    public void setDebugModeEnabled(boolean enabled) {
        redisService.setCacheObject(JbmConstants.SYS_CONFIG_DEBUG_MODE_KEY, enabled);
    }
}
