package com.jbm.framework.boot.autoconfigure.retrofit;

import com.alibaba.fastjson.support.retrofit.Retrofit2ConverterFactory;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import retrofit2.Retrofit;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author wesley
 */
public abstract class AbstractStrategy implements BaseStrategy {

    protected PlatformsProperties.Platform platform;

//    protected OkHttpClient okHttpClient;
//
//    protected Retrofit retrofit;

    @Autowired
    private ApplicationContext applicationContext;

    public AbstractStrategy() {
    }

    public <T> T getService(Class<T> service) {
       return applicationContext.getBean(service);
    }
    /**
     * @param platform
     */
    @Override
    public void setPlatform(PlatformsProperties.Platform platform) {
        this.platform = platform;
//        this.okHttpClient = new OkHttpClient.Builder()
//                .connectTimeout(30, TimeUnit.SECONDS)
//                .readTimeout(30, TimeUnit.SECONDS)
//                .writeTimeout(30, TimeUnit.SECONDS)
//                .build();
//        this.retrofit = new Retrofit.Builder()
//                .baseUrl(platform.getBaseUrl())
//                .client(okHttpClient)
//                .addConverterFactory(Retrofit2ConverterFactory.create())
//                .build();
    }

}
