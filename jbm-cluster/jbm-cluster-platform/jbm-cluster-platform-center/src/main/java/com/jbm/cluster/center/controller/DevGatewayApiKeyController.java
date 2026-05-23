package com.jbm.cluster.center.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.api.entitys.basic.BaseApiKey;
import com.jbm.cluster.common.mysql.service.BaseApiKeyService;
import com.jbm.cluster.common.mysql.service.BaseApiService;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * jaja7 本地联调：Gateway 直连 Center 查询 API Key / API 元数据（仅内部 Header）。
 */
@Profile("jaja7")
@RestController
@RequestMapping("/internal/gateway")
public class DevGatewayApiKeyController extends BaseController {

    @Autowired
    private BaseApiKeyService baseApiKeyService;
    @Autowired
    private BaseApiService baseApiService;

    @SaIgnore
    @GetMapping("/apikey")
    public ResultBody<BaseApiKey> getByApiKey(@RequestParam String apiKey, HttpServletRequest request) {
        if (!isInternal(request)) {
            return ResultBody.error("forbidden");
        }
        return ResultBody.callback(() -> baseApiKeyService.getByApiKey(apiKey));
    }

    @SaIgnore
    @GetMapping("/api")
    public ResultBody<BaseApi> findApiByPath(
            @RequestParam String serviceId,
            @RequestParam String path,
            HttpServletRequest request) {
        if (!isInternal(request)) {
            return ResultBody.error("forbidden");
        }
        return ResultBody.callback(() -> baseApiService.findApiByPath(serviceId, path));
    }

    @SaIgnore
    @GetMapping("/apikey/{keyId}/check")
    public ResultBody<Boolean> checkAuthority(
            @PathVariable Long keyId,
            @RequestParam Long apiId,
            HttpServletRequest request) {
        if (!isInternal(request)) {
            return ResultBody.error("forbidden");
        }
        return ResultBody.callback(() -> baseApiKeyService.hasAuthorityForApi(keyId, apiId));
    }

    private static boolean isInternal(HttpServletRequest request) {
        return StrUtil.isNotBlank(request.getHeader(JbmSecurityConstants.INTERNAL_SERVICE));
    }
}
