package com.jbm.cluster.auth.controller;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.common.security.JbmClusterHelper;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: wesley.zhang
 * @date: 2018/11/9 15:43
 * @description:
 */
@Api(tags = "管理员账号认证中心")
@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private TokenStore tokenStore;

    /**
     * 退出移除令牌
     */
    @ApiOperation(value = "退出并移除令牌", notes = "退出并移除令牌,令牌将失效")
    @GetMapping("/token/logout")
    public ResultBody removeToken() {
        String token = JbmClusterHelper.getCurrenToken();
        if (StrUtil.isEmpty(token)) {
            ResultBody.failed().msg("令牌异常");
        }
        tokenStore.removeAccessToken(tokenStore.readAccessToken(token));
        return ResultBody.ok().msg("退出成功");
    }


}
