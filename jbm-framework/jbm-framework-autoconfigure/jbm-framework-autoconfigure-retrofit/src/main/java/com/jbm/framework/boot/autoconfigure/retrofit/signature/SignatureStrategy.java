package com.jbm.framework.boot.autoconfigure.retrofit.signature;

import com.jbm.framework.boot.autoconfigure.retrofit.BaseStrategy;
import okhttp3.Request;

/**
 * @author wesley
 */ // 签名策略接口
public interface SignatureStrategy extends BaseStrategy {
    void generateSignature(Request originalRequest, Request.Builder signedRequest);
}