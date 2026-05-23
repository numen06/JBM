package com.jbm.cluster.platform.gateway.config;

import cn.dev33.satoken.id.SaIdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.api.entitys.basic.BaseApiKey;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * jaja7 本地联调：Gateway 直连 Center（127.0.0.1），避免 Feign/Nacos 依赖。
 */
@Slf4j
@Component
@Profile("jaja7")
public class Jaja7LocalCenterClient {

    @Value("${jbm.local.center-url:http://127.0.0.1:8888}")
    private String centerUrl;

    public BaseApiKey getByApiKey(String apiKey) {
        String url = centerUrl + "/internal/gateway/apikey?apiKey=" + cn.hutool.core.util.URLUtil.encode(apiKey);
        JSONObject root = getJson(url);
        if (root == null || !Boolean.TRUE.equals(root.getBool("success"))) {
            return null;
        }
        return root.getJSONObject("result").toBean(BaseApiKey.class);
    }

    public BaseApi findApiByPath(String serviceId, String path) {
        String url = centerUrl + "/internal/gateway/api?serviceId=" + serviceId + "&path=" + path;
        JSONObject root = getJson(url);
        if (root == null || !Boolean.TRUE.equals(root.getBool("success"))) {
            return null;
        }
        JSONObject result = root.getJSONObject("result");
        return result == null ? null : result.toBean(BaseApi.class);
    }

    public Boolean checkAuthority(Long keyId, Long apiId) {
        String url = centerUrl + "/internal/gateway/apikey/" + keyId + "/check?apiId=" + apiId;
        JSONObject root = getJson(url);
        if (root == null || !Boolean.TRUE.equals(root.getBool("success"))) {
            return false;
        }
        return root.getBool("result");
    }

    private JSONObject getJson(String url) {
        try {
            HttpResponse resp = HttpRequest.get(url)
                    .header(SaIdUtil.ID_TOKEN, SaIdUtil.getToken())
                    .header(JbmSecurityConstants.INTERNAL_SERVICE, "jbm-cluster-platform-gateway")
                    .header(JbmSecurityConstants.INTERNAL_INSTANCE, "jbm-cluster-platform-gateway:7777")
                    .timeout(8000)
                    .execute();
            if (resp.getStatus() != 200 || StrUtil.isBlank(resp.body())) {
                log.debug("[Jaja7LocalCenterClient] HTTP {} url={}", resp.getStatus(), url);
                return null;
            }
            return JSONUtil.parseObj(resp.body());
        } catch (Exception e) {
            log.debug("[Jaja7LocalCenterClient] request failed url={}: {}", url, e.getMessage());
            return null;
        }
    }
}
