package com.jbm.cluster.auth.controller;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.oauth2.model.AccessTokenModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Token 诊断接口
 * 用于排查 OAuth2 + Sa-Token 双层逻辑下的 token 过期问题
 */
@Slf4j
@Api(value = "Token诊断", tags = {"Token诊断管理"})
@RestController
@RequestMapping("/token/diagnose")
public class TokenDiagnoseController {

    @Autowired
    private RedisService redisService;

    /**
     * 获取 Sa-Token 配置快照
     * 对比 sa-token.properties 和 yml 中实际生效的配置
     */
    @ApiOperation("获取当前生效的Sa-Token配置")
    @GetMapping("/config")
    public ResultBody<Map<String, Object>> getConfig() {
        SaTokenConfig cfg = SaManager.getConfig();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sa-token.timeout(秒)", cfg.getTimeout());
        map.put("sa-token.timeout(小时)", cfg.getTimeout() / 3600.0);
        map.put("sa-token.activity-timeout(秒)", cfg.getActivityTimeout());
        map.put("sa-token.activity-timeout(小时)", cfg.getActivityTimeout() / 3600.0);
        map.put("sa-token.is-concurrent", cfg.getIsConcurrent());
        map.put("sa-token.is-share", cfg.getIsShare());
        map.put("sa-token.is-read-head", cfg.getIsReadHead());
        map.put("sa-token.is-read-cookie", cfg.getIsReadCookie());
        map.put("sa-token.token-prefix", cfg.getTokenPrefix());
        map.put("sa-token.token-name", cfg.getTokenName());
        map.put("sa-token.check-id-token", cfg.getCheckIdToken());
        map.put("sa-token.id-token-timeout(秒)", cfg.getIdTokenTimeout());
        map.put("sa-token.id-token-timeout(天)", cfg.getIdTokenTimeout() / 86400.0);

        // OAuth2 配置 (通过运行时环境读取，避免依赖额外 Bean)
        Map<String, Object> oauth2Map = new LinkedHashMap<>();
        oauth2Map.put("access-token-timeout(秒)", System.getProperty("sa-token.oauth2.access-token-timeout"));
        oauth2Map.put("client-token-timeout(秒)", System.getProperty("sa-token.oauth2.client-token-timeout"));
        map.put("OAuth2配置(运行时)", oauth2Map);

        return ResultBody.ok(map);
    }

    /**
     * 诊断指定 token 的双层过期状态
     * 传入 tokenValue，同时检查 Sa-Token 层和 OAuth2 层的 Redis key 和 TTL
     */
    @ApiOperation("诊断指定token的双层过期状态")
    @GetMapping("/check")
    public ResultBody<Map<String, Object>> checkToken(@RequestParam String tokenValue) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inputToken", tokenValue);
        String tokenName = SaManager.getConfig().getTokenName();
        if (StrUtil.isBlank(tokenName)) {
            tokenName = "satoken";
        }
        result.put("tokenNamePrefix", tokenName);

        // ========== 第1层：Sa-Token 层检查 ==========
        Map<String, Object> saTokenLayer = new LinkedHashMap<>();
        String saTokenKey = tokenName + ":login:token:" + tokenValue;
        String saTokenSessionKey = tokenName + ":login:session:" + tokenValue;
        String saTokenLastActivityKey = tokenName + ":login:last-activity:" + tokenValue;

        saTokenLayer.put("key_token", saTokenKey);
        saTokenLayer.put("key_session", saTokenSessionKey);
        saTokenLayer.put("key_last_activity", saTokenLastActivityKey);

        try {
            long tokenExpire = redisService.getExpire(saTokenKey);
            saTokenLayer.put("token_TTL(秒)", tokenExpire);
            saTokenLayer.put("token存在", tokenExpire > 0);
        } catch (Exception e) {
            saTokenLayer.put("token_TTL(秒)", "查询失败: " + e.getMessage());
        }

        try {
            long sessionExpire = redisService.getExpire(saTokenSessionKey);
            saTokenLayer.put("session_TTL(秒)", sessionExpire);
            saTokenLayer.put("session存在", sessionExpire > 0);
        } catch (Exception e) {
            saTokenLayer.put("session_TTL(秒)", "查询失败: " + e.getMessage());
        }

        try {
            long lastActivityExpire = redisService.getExpire(saTokenLastActivityKey);
            saTokenLayer.put("last_activity_TTL(秒)", lastActivityExpire);
            saTokenLayer.put("last_activity存在", lastActivityExpire > 0);
        } catch (Exception e) {
            saTokenLayer.put("last_activity_TTL(秒)", "查询失败: " + e.getMessage());
        }

        // 通过 Sa-Token API 检查
        try {
            Object loginId = StpUtil.getLoginIdByToken(tokenValue);
            saTokenLayer.put("loginId", loginId);
            // Sa-Token 1.32.0 没有 getTokenTimeoutByToken，用 Redis TTL 替代
            long ttl = redisService.getExpire(saTokenKey);
            saTokenLayer.put("tokenTimeout(从Redis)(秒)", ttl);
            long activityTtl = redisService.getExpire(saTokenLastActivityKey);
            saTokenLayer.put("activityTimeout(从Redis)(秒)", activityTtl);
        } catch (Exception e) {
            saTokenLayer.put("api检查异常", e.getMessage());
        }

        result.put("1_Sa-Token层", saTokenLayer);

        // ========== 第2层：OAuth2 层检查 ==========
        Map<String, Object> oauth2Layer = new LinkedHashMap<>();
        String oauth2AccessTokenKey = tokenName + ":oauth2:access-token:" + tokenValue;
        String oauth2RefreshTokenKey = tokenName + ":oauth2:refresh-token:" + tokenValue;

        oauth2Layer.put("key_access_token", oauth2AccessTokenKey);
        oauth2Layer.put("key_refresh_token", oauth2RefreshTokenKey);

        try {
            long accessTokenExpire = redisService.getExpire(oauth2AccessTokenKey);
            oauth2Layer.put("access_token_TTL(秒)", accessTokenExpire);
            oauth2Layer.put("access_token存在", accessTokenExpire > 0);
        } catch (Exception e) {
            oauth2Layer.put("access_token_TTL(秒)", "查询失败: " + e.getMessage());
        }

        try {
            long refreshTokenExpire = redisService.getExpire(oauth2RefreshTokenKey);
            oauth2Layer.put("refresh_token_TTL(秒)", refreshTokenExpire);
            oauth2Layer.put("refresh_token存在", refreshTokenExpire > 0);
        } catch (Exception e) {
            oauth2Layer.put("refresh_token_TTL(秒)", "查询失败: " + e.getMessage());
        }

        // 通过 OAuth2 API 检查
        try {
            AccessTokenModel accessTokenModel = SaOAuth2Util.getAccessToken(tokenValue);
            if (accessTokenModel != null) {
                oauth2Layer.put("OAuth2_AccessTokenModel", accessTokenModel.toLineMap());
            } else {
                oauth2Layer.put("OAuth2_AccessTokenModel", "null (不存在或已过期)");
            }
        } catch (Exception e) {
            oauth2Layer.put("OAuth2_API检查异常", e.getMessage());
        }

        result.put("2_OAuth2层", oauth2Layer);

        // ========== 第3层：在线用户表检查 ==========
        Map<String, Object> onlineLayer = new LinkedHashMap<>();
        String onlineKey = "online_tokens:" + tokenValue;
        try {
            long onlineExpire = redisService.getExpire(onlineKey);
            onlineLayer.put("online_key", onlineKey);
            onlineLayer.put("online_TTL(秒)", onlineExpire);
            onlineLayer.put("online存在", onlineExpire > 0);
            // 尝试获取在线用户详情
            Object onlineUser = redisService.getCacheObject(onlineKey);
            if (onlineUser != null) {
                onlineLayer.put("online_user", onlineUser.toString());
            }
        } catch (Exception e) {
            onlineLayer.put("online检查异常", e.getMessage());
        }
        result.put("3_在线用户表", onlineLayer);

        // ========== 诊断结论 ==========
        StringBuilder diagnosis = new StringBuilder();
        long saTokenTTL = -1;
        long oauth2TTL = -1;
        long onlineTTL = -1;
        try { saTokenTTL = redisService.getExpire(saTokenKey); } catch (Exception ignored) {}
        try { oauth2TTL = redisService.getExpire(oauth2AccessTokenKey); } catch (Exception ignored) {}
        try { onlineTTL = redisService.getExpire(onlineKey); } catch (Exception ignored) {}

        if (saTokenTTL <= 0 && oauth2TTL <= 0) {
            diagnosis.append("Token已完全过期（Sa-Token层和OAuth2层都不存在）");
        } else if (saTokenTTL > 0 && oauth2TTL <= 0) {
            diagnosis.append("问题! Sa-Token层存活但OAuth2层已过期。")
                    .append("请求会先通过Sa-Token校验，但OAuth2接口(如userinfo)会失败。")
                    .append("Sa-Token TTL=").append(saTokenTTL).append("s");
        } else if (saTokenTTL <= 0 && oauth2TTL > 0) {
            diagnosis.append("问题! OAuth2层存活但Sa-Token层已过期。")
                    .append("请求会被Sa-Token拦截返回401。")
                    .append("OAuth2 TTL=").append(oauth2TTL).append("s");
        } else {
            diagnosis.append("双层Token均有效。");
            if (saTokenTTL != oauth2TTL) {
                diagnosis.append("警告: 两层TTL不一致! Sa-Token=").append(saTokenTTL)
                        .append("s, OAuth2=").append(oauth2TTL).append("s")
                        .append("，较短的一方先过期会导致另一层成为孤儿数据。");
            } else {
                diagnosis.append("TTL一致，状态正常。");
            }
        }

        result.put("诊断结论", diagnosis.toString());
        return ResultBody.ok(result);
    }

    /**
     * 扫描 Redis 中所有 satoken 相关 key，查看 token 存储情况
     */
    @ApiOperation("扫描Redis中所有Sa-Token相关key")
    @GetMapping("/scan")
    public ResultBody<Map<String, Object>> scanRedisKeys(
            @RequestParam(defaultValue = "satoken:") String prefix,
            @RequestParam(defaultValue = "100") int limit) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Collection<String> keys = redisService.keys(prefix + "*");
            Map<String, Long> keyTTLMap = new LinkedHashMap<>();
            int count = 0;
            for (String key : keys) {
                if (count >= limit) break;
                long ttl = redisService.getExpire(key);
                keyTTLMap.put(key, ttl);
                count++;
            }
            result.put("总匹配key数", keys.size());
            result.put("返回数量", count);
            result.put("keys", keyTTLMap);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ResultBody.ok(result);
    }
}