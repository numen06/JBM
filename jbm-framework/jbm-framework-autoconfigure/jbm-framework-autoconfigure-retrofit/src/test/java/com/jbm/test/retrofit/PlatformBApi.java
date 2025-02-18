package com.jbm.test.retrofit;


import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import com.github.lianjiatech.retrofit.spring.boot.interceptor.Intercept;
import com.jbm.framework.boot.autoconfigure.retrofit.interceptor.AuthInterceptor;
import com.jbm.framework.boot.autoconfigure.retrofit.interceptor.SignatureInterceptor;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

@RetrofitClient(baseUrl = "${retrofit.platforms.platformB.base-url}")
@Intercept(handler = SignatureInterceptor.class)
@Intercept(handler = AuthInterceptor.class, exclude = "/token")
public interface PlatformBApi {

    @GET("/token")
    String token();

    @POST("/create")
    String create(@Body String body);

}