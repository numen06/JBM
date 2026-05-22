package com.jbm.cluster.center.controller;

import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Api(tags = "internal trust")
@RestController
@RequestMapping("/internal/trust")
public class InternalTrustTokenController {

    @Autowired(required = false)
    private SaOAuth2Template saOAuth2Template;

    @ApiOperation("issue ClientToken for feign trust test")
    @PostMapping("/client-token")
    public ResultBody<?> issueClientToken(@RequestParam(required = false) String clientId) {
        if (saOAuth2Template == null) {
            return ResultBody.failed().msg("SaOAuth2Template not ready");
        }
        String cid = StrUtil.blankToDefault(clientId, SpringUtil.getApplicationName());
        ClientTokenModel model = saOAuth2Template.generateClientToken(cid, "*");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("client_id", model.clientId);
        data.put("client_token", model.clientToken);
        return ResultBody.ok(data);
    }
}