package com.jbm.cluster.auth.controller;

import com.jbm.cluster.common.mysql.init.JbmJaja7SeedResetService;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * jaja7 本地联调：无需重启即可恢复 admin + JBM 应用 OAuth 默认凭证（勿用于生产）。
 */
@Api(tags = "jaja7 开发种子恢复")
@RestController
@Profile("jaja7")
@RequestMapping("/internal/dev")
@ConditionalOnProperty(name = "jbm.cluster.data-init.force-reset-default-password", havingValue = "true")
public class DevJaja7SeedController {

    @Autowired
    private JbmJaja7SeedResetService jbmJaja7SeedResetService;

    @ApiOperation("恢复 admin 密码与 JBM 种子应用 client_id/secret（明文见返回体）")
    @PostMapping("/reset-jaja7-seed")
    public ResultBody<Map<String, Object>> resetJaja7Seed() {
        return ResultBody.ok(jbmJaja7SeedResetService.resetAll());
    }
}
