package com.jbm.util.token;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TokenManagerRegistry: 管理多个平台的 TokenManager 实例。
 * 这是一个单例或 Spring Bean，负责注册、获取和管理不同平台的 TokenManager。
 */
public class TokenManagerRegistry {
    private final Map<String, TokenManager> platformManagers;
    private static final TokenManagerRegistry INSTANCE = new TokenManagerRegistry();

    private TokenManagerRegistry() {
        this.platformManagers = new ConcurrentHashMap<>();
    }

    public static TokenManagerRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 注册一个平台的 TokenManager。
     * @param platformId 平台唯一标识。
     * @param tokenManager 该平台的 TokenManager 实例。
     * @throws IllegalArgumentException 如果 platformId 为空或已存在。
     */
    public void registerTokenManager(String platformId, TokenManager tokenManager) {
        if (platformId == null || platformId.trim().isEmpty()) {
            throw new IllegalArgumentException("Platform ID cannot be null or empty.");
        }
        if (platformManagers.putIfAbsent(platformId, tokenManager) != null) {
            throw new IllegalArgumentException("TokenManager for platform '" + platformId + "' already registered.");
        }
        System.out.println("TokenManager registered for platform: " + platformId);
    }

    /**
     * 根据平台 ID 获取对应的 TokenManager。
     * @param platformId 平台唯一标识。
     * @return 对应的 TokenManager 实例。
     * @throws IllegalArgumentException 如果未找到指定平台的 TokenManager。
     */
    public TokenManager getTokenManager(String platformId) {
        TokenManager manager = platformManagers.get(platformId);
        if (manager == null) {
            throw new IllegalArgumentException("No TokenManager registered for platform: " + platformId);
        }
        return manager;
    }

    /**
     * 检查某个平台的 TokenManager 是否已注册。
     * @param platformId 平台唯一标识。
     * @return true 如果已注册。
     */
    public boolean isRegistered(String platformId) {
        return platformManagers.containsKey(platformId);
    }

    /**
     * 获取所有已注册的平台 ID。
     * @return 平台 ID 集合。
     */
    public Set<String> getAllPlatformIds() {
        return Collections.unmodifiableSet(platformManagers.keySet());
    }

    // 可选：提供批量刷新、状态检查等管理方法
}