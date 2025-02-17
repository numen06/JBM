package com.jbm.test.retrofit;


import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import retrofit2.http.GET;
import retrofit2.http.Header;

@RetrofitClient(baseUrl = "${platforms.platformA.base-url}")
public interface PlatformAApi {

    @GET("/data")
    String getData(@Header("Authorization") String authorization);

}