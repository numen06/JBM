package com.jbm.test.retrofit;


import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import com.github.lianjiatech.retrofit.spring.boot.interceptor.Intercept;
import com.jbm.util.proxy.annotation.RequestHeader;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

@RetrofitClient(baseUrl = "${platforms.platformB.base-url}")
@Intercept(handler = SignatureInterceptor.class, include = {"/**"}, exclude = "/token")
public interface PlatformBApi {

    @GET("/token")
    String token();

    @POST("/create")
    String create(@Body String body);

}