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
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

/**
 * @author wesley
 */
public abstract class AbstractInterceptor extends BasePathMatchInterceptor implements InitializingBean {
    @Resource
    protected StrategyFactory strategyFactory;
    @Resource
    protected PlatformsProperties platformsProperties;
    @Resource
    protected RetrofitProperties retrofitProperties;

    private final List<Class<? extends BaseStrategy>> strategys = new ArrayList<>();

    public AbstractInterceptor() {
    }


    public AbstractInterceptor(PlatformsProperties platformsProperties, RetrofitProperties retrofitProperties) {
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
    protected Response doIntercept(Interceptor.Chain chain) throws IOException {
        Request originalRequest = chain.request();
        if (MapUtil.isEmpty(platformsProperties.getPlatforms())) {
            return chain.proceed(originalRequest);
        }
//        Invocation invocation = Objects.requireNonNull(originalRequest.tag(Invocation.class));
        Request.Builder authRequestBuilder = originalRequest.newBuilder();
        this.strategys.forEach(strategyClass -> {
            BaseStrategy baseStrategy = strategyFactory.getStrategy(this, strategyClass);
            if (baseStrategy == null) {
                return;
            }
            baseStrategy.doStrategy(originalRequest, authRequestBuilder);
        });
        return chain.proceed(authRequestBuilder.build());
    }

    /**
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        ApiPlatform apiPlatform = AnnotationUtil.getAnnotation(this.getClass(), ApiPlatform.class);
        if (apiPlatform == null) {
            return;
        }
        this.strategys.addAll(Arrays.asList(apiPlatform.strategys()));
    }
}
