package com.jbm.test.retrofit;

import com.jbm.framework.boot.autoconfigure.retrofit.AbstractStrategy;
import com.jbm.framework.boot.autoconfigure.retrofit.signature.SignatureStrategy;
import okhttp3.Request;
import org.springframework.stereotype.Component;

@Component("HMAC_SHA256")
public class HMACSignatureSignatureStrategy extends AbstractStrategy implements SignatureStrategy {


    @Override
    public void generateSignature(Request originalRequest, Request.Builder signedRequest) {
        // 添加签名到请求头
        signedRequest
                .header("Authorization", "Bearer " + "test-token")
                .build();
    }

    /**
     * @param originalRequest
     * @param authRequestBuilder
     */
    @Override
    public void doStrategy(Request originalRequest, Request.Builder authRequestBuilder) {
        this.generateSignature(originalRequest, authRequestBuilder);
    }
}


