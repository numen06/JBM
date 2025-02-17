package com.jbm.test.retrofit.inf;

// 签名策略接口
public interface SignatureStrategy {
    String generateSignature(String data, String secretKey);
}