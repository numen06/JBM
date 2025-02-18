package com.jbm.framework.boot.autoconfigure.retrofit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * @author wesley
 */
public abstract class AbstractStrategy implements Strategy {

    protected PlatformsProperties.Platform platform;

    protected OkHttpClient client;

    protected Retrofit retrofit;

    public AbstractStrategy() {
    }

    public AbstractStrategy(PlatformsProperties.Platform platform) {
        this.platform = platform;
    }

    /**
     * @param platform
     */
    @Override
    public void setPlatform(PlatformsProperties.Platform platform) {
        this.platform = platform;
    }

    @Override
    public void setClient(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public void setRetrofit(Retrofit retrofit) {
        this.retrofit = retrofit;
    }
}
