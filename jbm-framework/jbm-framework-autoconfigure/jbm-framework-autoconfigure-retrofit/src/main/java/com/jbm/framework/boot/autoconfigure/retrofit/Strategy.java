package com.jbm.framework.boot.autoconfigure.retrofit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * @author wesley
 */
public interface Strategy {

    void setPlatform(PlatformsProperties.Platform platform);

}
