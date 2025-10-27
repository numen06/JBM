package com.jbm.cluster.auth.service;

import cn.dev33.satoken.oauth2.logic.SaOAuth2Consts;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.cluster.api.form.user.ThirdPartyUser;
import com.jbm.cluster.auth.config.ThirdPartyAuthProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ThirdPartyAuthService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Autowired
    private ThirdPartyAuthProperties thirdPartyAuthProperties;

    public ThirdPartyUser getUserInfoByCode(String code, String provider) {
        log.info("[第三方认证] 开始获取用户信息, provider: {}, code: {}", provider, code);
        
        ThirdPartyAuthProperties.PlatformConfig platformConfig = thirdPartyAuthProperties.getPlatforms().get(provider);
        if (platformConfig == null) {
            log.error("[第三方认证] 不支持的认证提供商: {}", provider);
            throw new RuntimeException("Unsupported provider: " + provider);
        }
        log.debug("[第三方认证] 获取平台配置成功, clientId: {}", platformConfig.getClientId());

        // Step 1: 换 access_token
        String tokenUrl = platformConfig.getTokenUrl();
        log.info("[第三方认证] Step 1: 开始获取access_token, tokenUrl: {}", tokenUrl);
        RequestBody body = new FormBody.Builder()
                .add("client_id", platformConfig.getClientId())
                .add("client_secret", platformConfig.getClientSecret())
                .add(SaOAuth2Consts.Param.grant_type, SaOAuth2Consts.GrantType.authorization_code)
                .add("code", code)
//                .add("redirect_uri", platformConfig.getRedirectUri())
                .build();

        //form方式请求
        Request tokenRequest = new Request.Builder()
                .url(tokenUrl)
                .post(body)
                .addHeader("Accept", "application/json")
                .build();

        String accessToken;
        try (Response tokenResponse = client.newCall(tokenRequest).execute()) {
            if (!tokenResponse.isSuccessful()) {
                log.error("[第三方认证] 获取access_token失败, 响应码: {}", tokenResponse.code());
                throw new IOException("Unexpected code " + tokenResponse);
            }

            String responseBody = Objects.requireNonNull(tokenResponse.body()).string();
            log.debug("[第三方认证] access_token响应: {}", responseBody);
            
            JSONObject tokenJson = JSON.parseObject(responseBody);
            if (tokenJson.get("result") != null) {
                accessToken = tokenJson.getJSONObject("result").getString("access_token");
            } else {
                accessToken = tokenJson.getString("access_token");
            }
            if (accessToken == null) {
                log.error("[第三方认证] 解析access_token失败, 响应体: {}", responseBody);
                throw new RuntimeException("No access token");
            }
            log.info("[第三方认证] Step 1: 获取access_token成功");
        } catch (IOException e) {
            log.error("[第三方认证] 获取access_token异常", e);
            throw new RuntimeException("Error getting access token", e);
        }

        // Step 2: 获取用户信息
        log.info("[第三方认证] Step 2: 开始获取用户信息, userInfoUrl: {}", platformConfig.getUserInfoUrl());
        Request userRequest = new Request.Builder()
                .url(platformConfig.getUserInfoUrl())
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response userResponse = client.newCall(userRequest).execute()) {
            if (!userResponse.isSuccessful()) {
                log.error("[第三方认证] 获取用户信息失败, 响应码: {}", userResponse.code());
                throw new IOException("Unexpected code " + userResponse);
            }

            String responseBody = Objects.requireNonNull(userResponse.body()).string();
            log.debug("[第三方认证] 用户信息响应: {}", responseBody);
            
            JSONObject userJson = JSON.parseObject(responseBody);
            if (userJson.get("result") != null) {
                userJson = userJson.getJSONObject("result");
            }

            ThirdPartyUser thirdPartyUser = ThirdPartyUser.builder()
                    .provider(provider)
                    .subjectId(getStringValue(userJson, "id", "openid", "userId", "userid", "sub"))
                    .email(userJson.getString("email"))
                    .username(getStringValue(userJson,"username","name"))
                    .mobile(userJson.getString("mobile"))
                    .nickname(getStringValue(userJson, "login", "nick","desc"))
                    .avatar(userJson.getString("avatar_url"))
                    .build();
            
            log.info("[第三方认证] Step 2: 获取用户信息成功, subjectId: {}, username: {}", 
                    thirdPartyUser.getSubjectId(), thirdPartyUser.getUsername());
            log.info("[第三方认证] 完成第三方用户信息获取, provider: {}", provider);
            
            return thirdPartyUser;
        } catch (IOException e) {
            log.error("[第三方认证] 获取用户信息异常", e);
            throw new RuntimeException("Error getting user info", e);
        }
    }

    /**
     * 从JSON对象中获取第一个非空字段值
     */
    private String getStringValue(JSONObject jsonObject, String... fieldNames) {
        log.debug("[第三方认证] 尝试获取字段值, 候选字段: {}", String.join(", ", fieldNames));
        for (String fieldName : fieldNames) {
            if (jsonObject.containsKey(fieldName)) {
                Object value = jsonObject.get(fieldName);
                if (value != null) {
                    log.debug("[第三方认证] 找到字段: {}, 值: {}", fieldName, value);
                    return value.toString();
                }
            }
        }
        log.debug("[第三方认证] 未找到任何候选字段的值");
        return null;
    }
}