package com.jbm.cluster.center.controller;

import cn.dev33.satoken.id.SaIdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.cluster.common.security.annotation.PermitAll;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Api(tags = "internal trust")
@RestController
@RequestMapping("/internal/trust")
public class InternalTrustTokenController {

    @PermitAll
    @ApiOperation("issue Id-Token for feign trust test")
    @PostMapping("/id-token")
    public ResultBody<?> issueIdToken() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", SpringUtil.getApplicationName());
        data.put("id_token", idToken());
        data.put("id_token_header", SaIdUtil.ID_TOKEN);
        return ResultBody.ok(data);
    }

    private static String idToken() {
        String token = SaIdUtil.getToken();
        if (StrUtil.isBlank(token)) {
            token = SaIdUtil.refreshToken();
        }
        return token;
    }
}
