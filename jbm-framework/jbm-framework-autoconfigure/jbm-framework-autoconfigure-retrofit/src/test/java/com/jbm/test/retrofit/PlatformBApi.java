package com.jbm.test.retrofit;


import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import com.github.lianjiatech.retrofit.spring.boot.interceptor.Intercept;
import com.github.lianjiatech.retrofit.spring.boot.interceptor.Intercepts;
import com.jbm.framework.boot.autoconfigure.retrofit.ApiPlatform;
import com.jbm.framework.boot.autoconfigure.retrofit.interceptor.AuthInterceptor;
import com.jbm.framework.boot.autoconfigure.retrofit.interceptor.SignatureInterceptor;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

@RetrofitClient(baseUrl = "${retrofit.platforms.platformB.base-url}")
@Intercepts({
        @Intercept(handler = SignatureInterceptor.class),
        @Intercept(handler = AuthInterceptor.class, exclude = "/token")
})
@ApiPlatform(name = "platformB", strategys = TestAuthStrategy.class)
public interface PlatformBApi {

    @GET("/token")
    String token();

    @POST("/create")
    String create(@Body String body);

}