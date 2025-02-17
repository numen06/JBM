package com.jbm.test.retrofit;

import com.github.lianjiatech.retrofit.spring.boot.interceptor.BasePathMatchInterceptor;
import com.jbm.test.retrofit.inf.SignatureStrategy;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SignatureInterceptor extends BasePathMatchInterceptor {

    @Autowired
    private SignatureStrategyFactory signatureStrategyFactory;

    @Override
    protected Response doIntercept(Chain chain) throws IOException {
        Request request = chain.request();
        String url = request.url().toString();
        String method = request.method();
        String body = request.body() != null ? request.body().toString() : "";
        // 生成签名
//        SignatureStrategy strategy = signatureStrategyFactory.getStrategy(signatureStrategy);
//        String signature = strategy.generateSignature(url + method + body, secretKey);
        String signature = "test-token";

        // 添加签名到请求头
        Request signedRequest = request.newBuilder()
                .header("Authorization", "Bearer " + signature)
                .build();

        return chain.proceed(signedRequest);
    }
}
