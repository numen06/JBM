package com.jbm.cluster.auth.controller;

import com.jbm.cluster.common.security.JbmClusterHelper;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;


@Api(tags = "当前帐号信息")
@RequestMapping("/current")
@RestController
public class CurrentUserController {

    /**
     * 获取用户基础信息
     *
     * @return
     */
    @ApiOperation(value = "获取当前登录用户信息", notes = "获取当前登录用户信息")
    @GetMapping("/user")
    public ResultBody getUserProfile() {
        return ResultBody.ok().data(SecurityUtils.getLoginUser());
    }


    /**
     * 获取当前登录用户信息-SSO单点登录
     *
     * @param principal
     * @return
     */
    @ApiOperation(value = "获取当前登录用户信息-SSO单点登录", notes = "获取当前登录用户信息-SSO单点登录")
    @GetMapping("/user/sso")
    public Principal principal(Principal principal) {
        return principal;
    }
}
