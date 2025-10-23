package com.jbm.cluster.auth.service;

import cn.dev33.satoken.oauth2.logic.SaOAuth2Consts;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.cluster.auth.config.ThirdPartyAuthProperties;
import com.jbm.cluster.api.form.user.ThirdPartyUser;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
        ThirdPartyAuthProperties.PlatformConfig platformConfig = thirdPartyAuthProperties.getPlatforms().get(provider);
        if (platformConfig == null) {
            throw new RuntimeException("Unsupported provider: " + provider);
        }
        
        // Step 1: 换 access_token
        String tokenUrl = platformConfig.getTokenUrl();
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
            if (!tokenResponse.isSuccessful()) throw new IOException("Unexpected code " + tokenResponse);
            
            String responseBody = Objects.requireNonNull(tokenResponse.body()).string();
            JSONObject tokenJson = JSON.parseObject(responseBody);
            if (tokenJson.get("result")!=null){
                accessToken =  tokenJson.getJSONObject("result").getString("access_token");
            }else {
                accessToken = tokenJson.getString("access_token");
            }
            if (accessToken == null) {
                throw new RuntimeException("No access token");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error getting access token", e);
        }

        // Step 2: 获取用户信息
        Request userRequest = new Request.Builder()
                .url(platformConfig.getUserInfoUrl())
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response userResponse = client.newCall(userRequest).execute()) {
            if (!userResponse.isSuccessful()) throw new IOException("Unexpected code " + userResponse);
            
            String responseBody = Objects.requireNonNull(userResponse.body()).string();
            JSONObject userJson = JSON.parseObject(responseBody);
            if (userJson.get("result")!=null){
                userJson = userJson.getJSONObject("result");
            }
            
            return ThirdPartyUser.builder()
                    .provider(provider)
                    .subjectId(getStringValue(userJson, "id", "openid", "userId","userid", "sub"))
                    .email(userJson.getString("email"))
                    .nickname(getStringValue(userJson, "login", "name", "username", "nick"))
                    .avatar(userJson.getString("avatar_url"))
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Error getting user info", e);
        }
    }
    
    /**
     * 从JSON对象中获取第一个非空字段值
     */
    private String getStringValue(JSONObject jsonObject, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (jsonObject.containsKey(fieldName)) {
                Object value = jsonObject.get(fieldName);
                if (value != null) {
                    return value.toString();
                }
            }
        }
        return null;
    }
}