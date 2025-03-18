package com.jbm.test.retrofit;

import com.jbm.framework.boot.autoconfigure.retrofit.AbstractStrategy;
import com.jbm.framework.boot.autoconfigure.retrofit.auth.AuthStrategy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

@Slf4j
public class TestAuthStrategy extends AbstractStrategy implements AuthStrategy {

    private static final String AUTH_TOKEN = "Bearer ";
    private static final String AUTH_HEARD = "Authorization";

    /**
     * @param originalRequest
     * @param authRequestBuilder
     */
    @Override
    public void generateToken(Request originalRequest, Request.Builder authRequestBuilder) {
        PlatformBApi platformBApi = this.getService(PlatformBApi.class);
        String token = platformBApi.token();
//        Request.Builder requestBuilder = new Request.Builder().url("http://localhost:8089/token");
//        String token = requestBuilder.get().build();
//        String token = "123456789";
        authRequestBuilder
                .header(AUTH_HEARD, AUTH_TOKEN + token)
                .build();

    }

    /**
     *
     */
    @Override
    public void clearToken() {

    }

    /**
     *
     */
    @Override
    public void refreshToken() {

    }


    /**
     * @param originalRequest
     * @param authRequestBuilder
     */
    @Override
    public void doStrategy(Request originalRequest, Request.Builder authRequestBuilder) {
        this.generateToken(originalRequest, authRequestBuilder);
    }
}


