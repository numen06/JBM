package com.jbm.framework.boot.autoconfigure.retrofit.signature;

import com.jbm.framework.boot.autoconfigure.retrofit.Strategy;
import okhttp3.Request;

/**
 * @author wesley
 */ // 签名策略接口
public interface SignatureStrategy extends Strategy {
    void generateSignature(Request originalRequest, Request.Builder signedRequest);
}