package com.jbm.framework.boot.autoconfigure.retrofit.interceptor;

import com.github.lianjiatech.retrofit.spring.boot.config.RetrofitProperties;
import com.jbm.framework.boot.autoconfigure.retrofit.PlatformsProperties;
import com.jbm.framework.boot.autoconfigure.retrofit.StrategyFactory;
import com.jbm.framework.boot.autoconfigure.retrofit.auth.AuthStrategy;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * @author wesley
 */
public class AuthInterceptor extends AbstractInterceptor {

    public AuthInterceptor(StrategyFactory strategyFactory, PlatformsProperties platformsProperties, RetrofitProperties retrofitProperties) {
        super(strategyFactory, platformsProperties, retrofitProperties);
    }

    /**
     * @param chain
     * @return
     * @throws IOException
     */
    @Override
    protected Response doIntercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        PlatformsProperties.Platform platform = getPlatform(originalRequest);
        // 获取平台信息
        if (platform == null) {
            return chain.proceed(originalRequest);
        }
        AuthStrategy authStrategy = strategyFactory.getStrategy(platform, AuthStrategy.class);
        if (authStrategy == null) {
            return chain.proceed(originalRequest);
        }
        Request.Builder authRequestBuilder = originalRequest.newBuilder();
        authStrategy.generateToken(originalRequest, authRequestBuilder);
        return chain.proceed(authRequestBuilder.build());
    }
}
