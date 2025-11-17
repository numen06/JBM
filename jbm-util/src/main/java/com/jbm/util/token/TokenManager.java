package com.jbm.util.token;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 通用的 HTTP Bearer Token 管理器。
 * 负责缓存 Token、自动刷新即将过期的 Token，并提供线程安全的访问。
 */
public class TokenManager {
    private final TokenProvider tokenProvider;
    private volatile TokenInfo currentToken; // 使用 volatile 保证可见性
    private final long refreshBufferSeconds; // 刷新缓冲期（秒），提前刷新
    private final ReentrantLock refreshLock = new ReentrantLock(); // 保证刷新操作的原子性


    public TokenManager(TokenProvider tokenProvider) {
        this(tokenProvider, 60); // 默认缓冲期 60 秒
    }
    /**
     * 构造函数。
     *
     * @param tokenProvider        提供 Token 获取和刷新逻辑的实现。
     * @param refreshBufferSeconds 在 Token 过期前多少秒开始尝试刷新。建议值：30-120 秒。
     */
    public TokenManager(TokenProvider tokenProvider, long refreshBufferSeconds) {
        this.tokenProvider = tokenProvider;
        this.refreshBufferSeconds = refreshBufferSeconds;
        // 初始化时尝试获取初始 Token
        try {
            this.currentToken = tokenProvider.getToken();
        } catch (TokenException e) {
            // 可以选择抛出，或记录日志并允许后续调用时重试
            System.err.println("Failed to initialize token: " + e.getMessage());
            this.currentToken = null; // 标记为需要重新获取
        }
    }

    /**
     * 获取当前有效的 Token 信息。
     * 如果 Token 为空、已过期或即将过期（在缓冲期内），则尝试刷新。
     *
     * @return 当前有效的 TokenInfo。
     * @throws TokenException 如果无法获取或刷新有效的 Token。
     */
    public TokenInfo getTokenInfo() throws TokenException {
        TokenInfo token = currentToken;

        // 检查缓存的 Token 是否有效且不需要立即刷新
        if (token != null && !token.isExpired() && !token.isExpiringSoon(refreshBufferSeconds)) {
            return token; // 直接返回缓存的 Token
        }

        // 需要刷新 Token
        return refreshTokenIfNeeded(false);
    }

    public String getTokenValue() throws TokenException {
        return getTokenInfo().getToken();
    }

    /**
     * 强制刷新 Token，无论当前状态如何。
     *
     * @return 新的 TokenInfo。
     * @throws TokenException 如果刷新失败。
     */
    public TokenInfo forceRefreshToken() throws TokenException {
        return refreshTokenIfNeeded(true);
    }

    /**
     * 内部方法：处理 Token 刷新逻辑。
     * 使用双重检查锁模式优化性能。
     *
     * @param force 强制刷新，忽略当前 Token 状态。
     * @return 新的 TokenInfo。
     * @throws TokenException 如果刷新失败。
     */
    private TokenInfo refreshTokenIfNeeded(Boolean force) throws TokenException {
        // 第一次检查（无锁）
        TokenInfo token = currentToken;
        if (token != null && !token.isExpired() && !token.isExpiringSoon(refreshBufferSeconds)) {
            return token;
        }

        // 获取锁进行刷新
        refreshLock.lock();
        try {
            // 第二次检查（有锁）
            token = currentToken;
            if (token != null && !token.isExpired() && !token.isExpiringSoon(refreshBufferSeconds)) {
                return token;
            }

            TokenInfo newToken;
            try {
                // 尝试刷新
                if (token != null && !token.isExpired()) {
                    // Token 尚未过期但即将过期，尝试用 refreshToken 刷新
                    newToken = tokenProvider.refreshToken();
                } else {
                    // Token 为空或已过期，尝试重新获取
                    newToken = tokenProvider.getToken();
                }
            } catch (TokenException e) {
                // 刷新/获取失败，如果当前有未过期的 Token，可以暂时返回旧的（但有风险）
                if (token != null && !token.isExpired()) {
                    System.err.println("Token refresh/get failed, using existing token as fallback: " + e.getMessage());
                    return token;
                }
                throw e; // 否则，抛出异常
            }

            if (newToken == null || newToken.getToken() == null || newToken.getToken().trim().isEmpty()) {
                throw new TokenException("Refreshed/Obtained token is null or empty.");
            }

            // 更新缓存
            this.currentToken = newToken;
            System.out.println("Token refreshed successfully. Expires at: " + newToken.getExpirationTime());
            return newToken;

        } finally {
            refreshLock.unlock();
        }
    }

    /**
     * 获取用于 HTTP 请求的 Authorization Header 值。
     * 格式: "Bearer <token>"
     *
     * @return Authorization Header 值。
     * @throws TokenException 如果无法获取有效的 Token。
     */
    public String getAuthorizationHeader() throws TokenException {
        return "Bearer " + getTokenInfo().getToken();
    }

    /**
     * 获取当前缓存的 Token 字符串（不进行刷新检查）。
     * 仅用于调试或监控，不保证其有效性。
     *
     * @return 当前缓存的 Token 字符串，可能为 null。
     */
    public String getCurrentTokenString() {
        TokenInfo token = currentToken;
        return token != null ? token.getToken() : null;
    }

    /**
     * 获取当前缓存的 Token 的过期时间（不进行刷新检查）。
     * 仅用于调试或监控。
     *
     * @return 当前缓存的 Token 的过期时间，可能为 null。
     */
    public Instant getCurrentTokenExpirationTime() {
        TokenInfo token = currentToken;
        return token != null ? token.getExpirationTime() : null;
    }
}