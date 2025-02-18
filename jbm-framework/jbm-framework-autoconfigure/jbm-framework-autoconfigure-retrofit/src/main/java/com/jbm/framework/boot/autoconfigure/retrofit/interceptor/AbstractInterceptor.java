package com.jbm.framework.boot.autoconfigure.retrofit.interceptor;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.StrUtil;
import com.github.lianjiatech.retrofit.spring.boot.config.RetrofitProperties;
import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import com.github.lianjiatech.retrofit.spring.boot.interceptor.BasePathMatchInterceptor;
import com.jbm.framework.boot.autoconfigure.retrofit.PlatformsProperties;
import com.jbm.framework.boot.autoconfigure.retrofit.StrategyFactory;
import okhttp3.Request;
import retrofit2.Invocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        return getPlatform(url);
    }
    public PlatformsProperties.Platform getPlatform(String  url) {
        return platformsProperties.getPlatforms().values().stream().filter(platform -> StrUtil.contains(url, platform.getBaseUrl())).findFirst().orElse(null);
    }

    private final Map<Class<?>,PlatformsProperties.Platform> CLASS_PLATFORM_CACHE = new ConcurrentHashMap<>();

    public PlatformsProperties.Platform getPlatform(Invocation invocation, Request request) {
        Class<? extends Object> declaringClass = invocation.method().getDeclaringClass();
        if (CLASS_PLATFORM_CACHE.containsKey(declaringClass)) {
            return CLASS_PLATFORM_CACHE.get(declaringClass);
        }
        String baseUrl = AnnotationUtil.getAnnotation(declaringClass, RetrofitClient.class).baseUrl();
        if (StrUtil.isEmpty(baseUrl)) {
            return null;
        }
        if (StrUtil.startWith(baseUrl, "${")) {
            String platformName = StrUtil.subBetween(baseUrl, "${retrofit.platforms.", ".base-url}");
            PlatformsProperties.Platform platform = platformsProperties.getPlatforms().get(platformName);
            CLASS_PLATFORM_CACHE.put(declaringClass, platform);
            return platform;
        }
        return this.getPlatform(request);
    }
}
