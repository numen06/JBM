package com.jbm.cluster.auth.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.service.feign.client.BaseAppServiceClient;
import com.jbm.cluster.core.constant.JbmCacheConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * APP信息调用预处理
 *
 * @Created wesley.zhang
 * @Date 2022/6/17 11:43
 * @Description TODO
 */
@Slf4j
@Service
public class BaseAppPreprocessing {

    @Autowired
    private BaseAppServiceClient baseAppServiceClient;

    @Cacheable(value = JbmCacheConstants.APP_CACHE_NAMESPACE, key = "#appKey", unless = "#result == null")
    public BaseApp getAppByKey(String appKey) {
        if (StrUtil.contains(appKey, "-")) {
            BaseApp app = new BaseApp();
            app.setAppId(0L);
            app.setApiKey(appKey);
            app.setSecretKey(appKey);
            return app;
        }
        BaseApp baseApp = baseAppServiceClient.getAppByKey(appKey).getResult();
        if (ObjectUtil.isEmpty(baseApp)) {
            throw new NullPointerException("应用不存在");
        }
        return baseApp;
    }

    @Autowired
    private CacheManager cacheManager;

    /**
     * 清理缓存
     */
    @PostConstruct
    public void clearCache() {
        if (cacheManager.getCache(JbmCacheConstants.APP_CACHE_NAMESPACE) != null) {
            log.info("清理{}缓存", JbmCacheConstants.APP_CACHE_NAMESPACE);
            Objects.requireNonNull(cacheManager.getCache(JbmCacheConstants.APP_CACHE_NAMESPACE)).clear();
        }
    }


}
