package com.jbm.cluster.api.constants;

/**
 * OAuth2 客户端密钥校验（支持 BCrypt 存储的 secretKey）
 */
public interface OAuthClientSecretVerifier {

    /**
     * @param clientId     通常为 apiKey
     * @param clientSecret 请求中的明文密钥
     * @return 是否匹配
     */
    boolean verify(String clientId, String clientSecret);
}
