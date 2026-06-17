package com.jbm.cluster.platform.gateway.service;

import com.jbm.cluster.core.constant.JbmConstants;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 系统调试模式（环境变量 / Redis，与 common-basic 共享同一 Redis key）
 */
@Service
public class SysDebugModeService {

    @Autowired
    private RedisService redisService;

    @Value("${jbm.debug:false}")
    private boolean envDebugEnabled;

    public boolean isDebugModeEnabled() {
        return envDebugEnabled || isRedisDebugModeEnabled();
    }

    private boolean isRedisDebugModeEnabled() {
        Boolean val = redisService.getCacheObject(JbmConstants.SYS_CONFIG_DEBUG_MODE_KEY);
        return Boolean.TRUE.equals(val);
    }
}
