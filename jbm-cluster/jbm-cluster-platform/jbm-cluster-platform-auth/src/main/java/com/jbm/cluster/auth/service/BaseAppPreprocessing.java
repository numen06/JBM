package com.jbm.cluster.auth.service;

import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.common.mysql.service.BaseAppService;
import com.jbm.cluster.core.constant.JbmCacheConstants;
import com.jbm.framework.exceptions.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * APP 信息本地查询（OAuth2 Client / RSA 密钥）
 */
@Slf4j
@Service
public class BaseAppPreprocessing {

    @Autowired
    private BaseAppService baseAppService;

    @Autowired
    private CacheManager cacheManager;

    @Cacheable(value = JbmCacheConstants.APP_CACHE_NAMESPACE, key = "#appKey", unless = "#result == null")
    public BaseApp getAppByKey(String appKey) {
        BaseApp baseApp = baseAppService.getAppInfoByKey(appKey);
        if (ObjectUtil.isEmpty(baseApp)) {
            throw new ServiceException("应用不存在: " + appKey);
        }
        return baseApp;
    }

    @PostConstruct
    public void clearCache() {
        try {
            if (cacheManager.getCache(JbmCacheConstants.APP_CACHE_NAMESPACE) != null) {
                log.info("清理{}缓存", JbmCacheConstants.APP_CACHE_NAMESPACE);
                Objects.requireNonNull(cacheManager.getCache(JbmCacheConstants.APP_CACHE_NAMESPACE)).clear();
            }
        } catch (Exception ex) {
            log.warn("启动时清理 {} 缓存失败（Redis 不可用时不阻断启动）: {}",
                    JbmCacheConstants.APP_CACHE_NAMESPACE, ex.getMessage());
        }
    }
}
