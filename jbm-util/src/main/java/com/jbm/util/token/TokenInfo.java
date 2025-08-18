package com.jbm.util.token;

import lombok.Data;

import java.time.Instant;

/**
 * 封装 Token 信息，包括 Token 字符串和过期时间戳。
 */
@Data
public class TokenInfo {
    private final String token;
    private final Instant expirationTime; // Token 的过期时间点

    public TokenInfo(String token, Instant expirationTime) {
        this.token = token;
        this.expirationTime = expirationTime;
    }

    public TokenInfo(String token) {
        //默认1天
        this(token, Instant.now().plusSeconds(86400));
    }

    /**
     * 判断 Token 是否已过期。
     *
     * @return true 如果当前时间已超过过期时间。
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expirationTime);
    }

    /**
     * 判断 Token 是否在指定的缓冲期内即将过期。
     * 例如，如果缓冲期是 60 秒，那么在过期前 60 秒内返回 true。
     *
     * @param bufferSeconds 缓冲秒数。
     * @return true 如果 Token 即将在缓冲期内过期。
     */
    public boolean isExpiringSoon(long bufferSeconds) {
        Instant now = Instant.now();
        Instant bufferTime = expirationTime.minusSeconds(bufferSeconds);
        return now.isAfter(bufferTime);
    }
}
