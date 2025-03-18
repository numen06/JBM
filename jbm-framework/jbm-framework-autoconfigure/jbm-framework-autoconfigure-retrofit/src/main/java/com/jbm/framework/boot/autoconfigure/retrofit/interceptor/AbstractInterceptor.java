package com.jbm.framework.boot.autoconfigure.retrofit.interceptor;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.github.lianjiatech.retrofit.spring.boot.config.RetrofitProperties;
import com.github.lianjiatech.retrofit.spring.boot.interceptor.BasePathMatchInterceptor;
import com.jbm.framework.boot.autoconfigure.retrofit.ApiPlatform;
import com.jbm.framework.boot.autoconfigure.retrofit.BaseStrategy;
import com.jbm.framework.boot.autoconfigure.retrofit.PlatformsProperties;
import com.jbm.framework.boot.autoconfigure.retrofit.StrategyFactory;
import com.jbm.framework.boot.autoconfigure.retrofit.auth.AuthStrategy;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Invocation;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wesley
 */
public abstract class AbstractInterceptor extends BasePathMatchInterceptor {

    protected final StrategyFactory strategyFactory;
    protected final PlatformsProperties platformsProperties;
    protected final RetrofitProperties retrofitProperties;

    public AbstractInterceptor(StrategyFactory strategyFactory, PlatformsProperties platformsProperties, RetrofitProperties retrofitProperties) {
        this.strategyFactory = strategyFactory;
        this.platformsProperties = platformsProperties;
        this.retrofitProperties = retrofitProperties;
    }

    public PlatformsProperties.Platform getPlatform(Request request) {
        String url = request.url().toString();
        return getPlatform(url);
    }

    public PlatformsProperties.Platform getPlatform(String url) {
        return platformsProperties.getPlatforms().values().stream().filter(platform -> StrUtil.contains(url, platform.getBaseUrl())).findFirst().orElse(null);
    }


    @Override
    protected Response doIntercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        if (MapUtil.isEmpty(platformsProperties.getPlatforms())) {
            return chain.proceed(originalRequest);
        }
        Invocation invocation = Objects.requireNonNull(originalRequest.tag(Invocation.class));
        AuthStrategy authStrategy = strategyFactory.getStrategy(invocation, AuthStrategy.class);
        if (authStrategy == null) {
            return chain.proceed(originalRequest);
        }
        Request.Builder authRequestBuilder = originalRequest.newBuilder();
        authStrategy.generateToken(originalRequest, authRequestBuilder);
        return chain.proceed(authRequestBuilder.build());
    }


    protected abstract void doIntercept2(List<BaseStrategy> strategyList, Request originalRequest, Request.Builder authRequestBuilder) ;



}
