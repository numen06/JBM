package com.jbm.test.retrofit;

import com.jbm.framework.boot.autoconfigure.retrofit.signature.AbstractSinatureStrategy;
import okhttp3.Request;
import org.springframework.stereotype.Component;

@Component("HMAC_SHA256")
public class HMACSignatureStrategy extends AbstractSinatureStrategy {


    @Override
    public void generateSignature(Request originalRequest, Request.Builder signedRequest) {
        // 添加签名到请求头
        signedRequest
                .header("Authorization", "Bearer " + "test-token")
                .build();
    }
}


