package com.jbm.cluster.auth.service;

import cn.dev33.satoken.oauth2.logic.SaOAuth2Consts;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.cluster.api.form.user.ThirdPartyUser;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.auth.config.ThirdPartyAuthProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ThirdPartyAuthService {

    private final OkHttpClient client = createOkHttpClient();

    private OkHttpClient createOkHttpClient() {
        try {
            // Create a trust manager that trusts all certificates (USE WITH CAUTION)
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            // Trust all client certificates
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            // Trust all server certificates
                            // WARNING: This is insecure for production use
                            log.warn("[第三方认证] 使用自定义SSL信任管理器 - 仅用于开发/测试环境");
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> {
                        // Trust all hostnames (USE WITH CAUTION)
                        log.debug("[第三方认证] 跳过主机名验证: {}", hostname);
                        return true;
                    })
                    .build();
        } catch (Exception e) {
            log.error("[第三方认证] 创建OkHttpClient失败", e);
            throw new RuntimeException("Failed to create OkHttpClient", e);
        }
    }

    @Autowired
    private ThirdPartyAuthProperties thirdPartyAuthProperties;

    public String logout(String provider, JbmLoginUser loginUser) {
        if (provider == null) {
            return null;
        }
        ThirdPartyAuthProperties.PlatformConfig platformConfig = thirdPartyAuthProperties.getPlatforms().get(provider);
        if (platformConfig == null) {
            return null;
        }
        String logoutUrl = platformConfig.getLogoutUrl();
        //调用第三方的登出接口get请求带token
        if (StrUtil.isEmpty(logoutUrl)) {
            return platformConfig.getLoginUrl();
        }
        if (StrUtil.isEmpty(loginUser.getThirdToken())) {
            log.info("[第三方认证] 登出失败, 未获取到第三方token");
            return platformConfig.getLoginUrl();
        }
        log.info("[第三方认证] 登出, logoutUrl: {}, token :{}", logoutUrl, loginUser.getThirdToken());
        //设置url参数access_token
        HttpUrl.Builder urlBuilder = HttpUrl.parse(logoutUrl).newBuilder();
        urlBuilder.addQueryParameter("access_token", loginUser.getThirdToken());
        // 2. 创建 Request
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get() // GET 请求
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("[第三方认证] 登出失败, 响应码: {}", response.code());
                throw new IOException("Unexpected code " + response);
            }
            if (response.body() != null) {
                log.info("[第三方认证] 登出成功, 响应: {}", response.body().string());
            }
        } catch (IOException e) {
            log.error("[第三方认证] 登出失败", e);
        }
        return platformConfig.getLoginUrl();
    }

    public ThirdPartyUser getUserInfoByCode(String code, String redirectUri, String provider) {
        log.info("[第三方认证] 开始获取用户信息, provider: {}, code: {}", provider, code);

        ThirdPartyAuthProperties.PlatformConfig platformConfig = thirdPartyAuthProperties.getPlatforms().get(provider);
        if (platformConfig == null) {
            log.error("[第三方认证] 不支持的认证提供商: {}", provider);
            throw new RuntimeException("Unsupported provider: " + provider);
        }
        log.info("[第三方认证] 获取平台配置成功, 配置信息: {}", platformConfig);

        // Step 1: 换 access_token
        String tokenUrl = platformConfig.getTokenUrl();
        log.info("[第三方认证] Step 1: 开始获取access_token, tokenUrl: {}", tokenUrl);
        log.info("[第三方认证] Step 1: 请求参数 - clientId: {}, code: {}", platformConfig.getClientId(), code);

        HttpUrl.Builder tokenUrlBuilder = Objects.requireNonNull(HttpUrl.parse(tokenUrl)).newBuilder()
                .addQueryParameter("client_id", platformConfig.getClientId())
                .addQueryParameter("client_secret", platformConfig.getClientSecret())
                .addQueryParameter(SaOAuth2Consts.Param.grant_type, SaOAuth2Consts.GrantType.authorization_code)
                .addQueryParameter("code", code);

        //OAuth2规范要求：如果获取授权码时使用了redirect_uri，换取token时也必须提供相同的redirect_uri
        if (StrUtil.isNotBlank(redirectUri)) {
            tokenUrlBuilder.addQueryParameter("redirect_uri", redirectUri);
            log.info("[第三方认证] Step 1: 添加redirect_uri参数: {}", platformConfig.getRedirectUri());
        }

        HttpUrl finalTokenUrl = tokenUrlBuilder.build();

        Request tokenRequest = new Request.Builder()
                .url(finalTokenUrl)
                .get()
                .addHeader("Accept", "application/json")
                .build();

        log.info("[第三方认证] Step 1: 准备发送请求到: {}, 方法: POST", tokenUrl);

        String accessToken;
        Response tokenResponse = null;
        try {
            log.info("[第三方认证] Step 1: 开始执行HTTP请求...");
            tokenResponse = client.newCall(tokenRequest).execute();
            log.info("[第三方认证] Step 1: HTTP请求执行完成, 状态码: {}", tokenResponse.code());

            String responseBody = Objects.requireNonNull(tokenResponse.body()).string();
            log.info("[第三方认证] Step 1: 响应体长度: {} 字节", responseBody.length());
            log.info("[第三方认证] Step 1: 完整响应体: {}", responseBody);

            // 检查响应体是否为空
            if (StrUtil.isBlank(responseBody)) {
                log.error("[第三方认证] 响应体为空");
                throw new RuntimeException("Response body is empty");
            }

            // 检查响应是否包含错误信息（即使HTTP状态码是200，也可能返回错误JSON）
            JSONObject responseJson;
            try {
                responseJson = JSON.parseObject(responseBody);
            } catch (Exception e) {
                log.error("[第三方认证] 解析响应体JSON失败, 响应体: {}", responseBody, e);
                throw new RuntimeException("Failed to parse response JSON: " + responseBody, e);
            }
            if (responseJson.containsKey("errcode") || responseJson.containsKey("error")) {
                String errcode = responseJson.getString("errcode");
                String errmsg = responseJson.getString("errmsg");
                String error = responseJson.getString("error");
                String errorDescription = responseJson.getString("error_description");
                log.error("[第三方认证] 获取access_token失败, 错误码: {}, 错误信息: {}, error: {}, error_description: {}",
                        errcode, errmsg, error, errorDescription);
                log.error("[第三方认证] 完整响应体: {}", responseBody);
                log.error("[第三方认证] 注意: 虽然请求已发送并收到响应, 但code可能未被服务器标记为已使用, 可以重试");
                throw new RuntimeException(String.format("获取access_token失败: %s - %s",
                        errcode != null ? errcode : error,
                        errmsg != null ? errmsg : errorDescription));
            }

            if (!tokenResponse.isSuccessful()) {
                log.error("[第三方认证] 获取access_token失败, 响应码: {}, 响应体: {}", tokenResponse.code(), responseBody);
                log.error("[第三方认证] 注意: HTTP状态码异常, code可能未被服务器标记为已使用");
                throw new IOException("Unexpected code " + tokenResponse.code() + ", response: " + responseBody);
            }

            log.info("[第三方认证] Step 1: access_token响应: {}", responseBody);

            // 尝试多种可能的响应格式来获取access_token
            JSONObject tokenJson = responseJson;
            accessToken = null;

            // 格式1: {"result": {"access_token": "xxx"}}
            if (tokenJson.get("result") != null) {
                Object resultObj = tokenJson.get("result");
                if (resultObj instanceof JSONObject) {
                    accessToken = ((JSONObject) resultObj).getString("access_token");
                    log.debug("[第三方认证] 从result.access_token获取token");
                }
            }

            // 格式2: {"access_token": "xxx"}
            if (accessToken == null) {
                accessToken = tokenJson.getString("access_token");
                if (accessToken != null) {
                    log.debug("[第三方认证] 从access_token获取token");
                }
            }

            // 格式3: {"data": {"access_token": "xxx"}}
            if (accessToken == null && tokenJson.get("data") != null) {
                Object dataObj = tokenJson.get("data");
                if (dataObj instanceof JSONObject) {
                    accessToken = ((JSONObject) dataObj).getString("access_token");
                    if (accessToken != null) {
                        log.debug("[第三方认证] 从data.access_token获取token");
                    }
                }
            }

            // 格式4: {"token": "xxx"} 或 {"token_type": "Bearer", "token": "xxx"}
            if (accessToken == null) {
                accessToken = tokenJson.getString("token");
                if (accessToken != null) {
                    log.debug("[第三方认证] 从token获取token");
                }
            }

            if (accessToken == null) {
                log.error("[第三方认证] 解析access_token失败, 响应体中未找到access_token字段");
                log.error("[第三方认证] 完整响应体: {}", responseBody);
                log.error("[第三方认证] 响应体中的所有键: {}", tokenJson.keySet());

                // 尝试输出所有可能包含token的字段值（前100个字符）
                for (String key : tokenJson.keySet()) {
                    Object value = tokenJson.get(key);
                    if (value != null) {
                        String valueStr = value.toString();
                        if (valueStr.length() > 100) {
                            valueStr = valueStr.substring(0, 100) + "...";
                        }
                        log.error("[第三方认证] 响应字段 - {}: {}", key, valueStr);
                    }
                }

                throw new RuntimeException("No access token in response. Response body: " + responseBody);
            }
            log.info("[第三方认证] Step 1: 获取access_token成功, accessToken: {}",
                    accessToken != null ? accessToken.substring(0, Math.min(20, accessToken.length())) + "..." : "null");
        } catch (IOException e) {
            log.error("[第三方认证] 获取access_token IO异常, 异常类型: {}, 异常信息: {}",
                    e.getClass().getName(), e.getMessage());
            log.error("[第三方认证] 注意: 如果是网络异常导致请求未到达服务器, code可能未被使用, 可以重试", e);
            throw new RuntimeException("Error getting access token: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[第三方认证] 获取access_token未知异常, 异常类型: {}, 异常信息: {}",
                    e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("Unexpected error getting access token: " + e.getMessage(), e);
        } finally {
            if (tokenResponse != null) {
                try {
                    tokenResponse.close();
                } catch (Exception e) {
                    log.warn("[第三方认证] 关闭响应流异常", e);
                }
            }
        }

        // Step 2: 获取用户信息
        log.info("[第三方认证] Step 2: 开始获取用户信息, userInfoUrl: {}", platformConfig.getUserInfoUrl());
        //access_token做成参数，通过get请求
        HttpUrl userInfoUrl = Objects.requireNonNull(HttpUrl.parse(platformConfig.getUserInfoUrl()))
                .newBuilder()
                .addQueryParameter("access_token", accessToken)
                .build();

        Request userRequest = new Request.Builder()
                .url(userInfoUrl)
                .get()
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
                    .token(accessToken)
                    .clientId(platformConfig.getClientId())
                    .provider(provider)
                    .subjectId(getStringValue(userJson, "id", "openid", "userId", "userid", "sub"))
                    .email(userJson.getString("email"))
                    .username(getStringValue(userJson, "username", "name"))
                    .mobile(userJson.getString("mobile"))
                    .nickname(getStringValue(userJson, "login", "nick", "desc"))
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