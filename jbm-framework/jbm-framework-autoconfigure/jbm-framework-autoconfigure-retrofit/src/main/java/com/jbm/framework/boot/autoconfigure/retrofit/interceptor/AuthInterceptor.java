package com.jbm.framework.boot.autoconfigure.retrofit.interceptor;

import cn.hutool.core.map.MapUtil;
import com.github.lianjiatech.retrofit.spring.boot.config.RetrofitProperties;
import com.jbm.framework.boot.autoconfigure.retrofit.PlatformsProperties;
import com.jbm.framework.boot.autoconfigure.retrofit.StrategyFactory;
import com.jbm.framework.boot.autoconfigure.retrofit.auth.AuthStrategy;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Invocation;

import java.io.IOException;
import java.util.Objects;

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
}
