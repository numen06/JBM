package com.jbm.framework.boot.autoconfigure.retrofit.interceptor;

import cn.hutool.core.map.MapUtil;
import com.github.lianjiatech.retrofit.spring.boot.config.RetrofitProperties;
import com.jbm.framework.boot.autoconfigure.retrofit.PlatformsProperties;
import com.jbm.framework.boot.autoconfigure.retrofit.StrategyFactory;
import com.jbm.framework.boot.autoconfigure.retrofit.signature.SignatureStrategy;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Invocation;

import java.io.IOException;
import java.util.Objects;

/**
 * @author wesley
 */
public class SignatureInterceptor extends AbstractInterceptor {


    public SignatureInterceptor(StrategyFactory strategyFactory, PlatformsProperties platformsProperties, RetrofitProperties retrofitProperties) {
        super(strategyFactory, platformsProperties, retrofitProperties);
    }


    @Override
    protected Response doIntercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        if (MapUtil.isEmpty(platformsProperties.getPlatforms())) {
            return chain.proceed(originalRequest);
        }
        Invocation invocation = Objects.requireNonNull(originalRequest.tag(Invocation.class));
        SignatureStrategy strategy = strategyFactory.getStrategy(invocation, SignatureStrategy.class);
        if (strategy == null) {
            return chain.proceed(originalRequest);
        }
        Request.Builder signedRequestBuilder = originalRequest.newBuilder();
        strategy.generateSignature(originalRequest, signedRequestBuilder);
        return chain.proceed(signedRequestBuilder.build());
    }

}
