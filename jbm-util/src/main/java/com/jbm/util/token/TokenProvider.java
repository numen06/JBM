package com.jbm.util.token;

import java.time.Instant;

/**
 * TokenProvider 定义了获取 Token 及其过期时间的接口。
 * 具体实现可以是从配置文件读取、调用 OAuth2 API、从环境变量获取等。
 */
public interface TokenProvider {

    /**
     * 获取当前有效的 Token。
     * @return 包含 Token 字符串和其过期时间的 TokenInfo 对象。
     * @throws TokenException 如果获取 Token 失败（如网络错误、认证失败）。
     */
    TokenInfo getToken() throws TokenException;

    /**
     * 刷新 Token。
     * 当 Token 过期或即将过期时，TokenManager 会调用此方法。
     * @return 包含新 Token 字符串和其过期时间的 TokenInfo 对象。
     * @throws TokenException 如果刷新 Token 失败。
     */
    TokenInfo refreshToken() throws TokenException;


}

