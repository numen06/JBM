package com.jbm.framework.boot.autoconfigure.retrofit.interceptor;

import cn.hutool.core.util.StrUtil;
import com.github.lianjiatech.retrofit.spring.boot.config.RetrofitProperties;
import com.github.lianjiatech.retrofit.spring.boot.interceptor.BasePathMatchInterceptor;
import com.jbm.framework.boot.autoconfigure.retrofit.PlatformsProperties;
import com.jbm.framework.boot.autoconfigure.retrofit.StrategyFactory;
import okhttp3.Request;

/**
 * @author wesley
 */
public abstract class AbstractInterceptor extends BasePathMatchInterceptor {

    protected final StrategyFactory strategyFactory;
    protected final PlatformsProperties platformsProperties;
    protected final RetrofitProperties retrofitProperties;

    public AbstractInterceptor(StrategyFactory strategyFactory,PlatformsProperties platformsProperties, RetrofitProperties retrofitProperties) {
        this.strategyFactory = strategyFactory;
        this.platformsProperties = platformsProperties;
        this.retrofitProperties = retrofitProperties;
    }

    public PlatformsProperties.Platform getPlatform(Request request) {
        String url = request.url().toString();
        return platformsProperties.getPlatforms().values().stream().filter(platform -> StrUtil.contains(url, platform.getBaseUrl())).findFirst().orElse(null);
    }
}
