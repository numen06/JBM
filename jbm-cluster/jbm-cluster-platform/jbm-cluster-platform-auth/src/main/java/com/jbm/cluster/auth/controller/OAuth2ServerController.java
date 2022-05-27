package com.jbm.cluster.auth.controller;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Consts;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Handle;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.auth.service.SysLoginService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Created wesley.zhang
 * @Date 2022/5/15 10:18
 * @Description TODO
 */
@RestController
@RequestMapping("/oauth2")
public class OAuth2ServerController {

    @Autowired
    private SysLoginService sysLoginService;

    // 处理所有OAuth相关请求
    public Object oauth2() {
        System.out.println("------- 进入请求: " + SaHolder.getRequest().getUrl());
        Object result = SaOAuth2Handle.serverRequest();
        if (SaOAuth2Consts.NOT_HANDLE.equals(result)) {
            return ResultBody.failed().httpStatus(400).code(400).msg("输入参数错误,没有找到匹配的授权模式");
        }
        if (result instanceof SaResult) {
            SaResult saResult = (SaResult) result;
            if (SaResult.CODE_SUCCESS == saResult.getCode()) {
                return ResultBody.ok().data(saResult.getData());
            } else {
                return ResultBody.failed().data(saResult.getData()).msg(saResult.getMsg());
            }
        }
        return result;
    }

    // 处理所有OAuth相关请求
    @RequestMapping("/authorize")
    public Object oauth() {
        return this.oauth2();
    }


    // 处理所有OAuth相关请求
    @RequestMapping("/token")
    public Object token() {
        return this.oauth2();
    }

    // 处理所有OAuth相关请求
    @RequestMapping("/refresh")
    public Object refresh() {
        return this.oauth2();
    }

    // 处理所有OAuth相关请求
    @RequestMapping("/client_token")
    public Object client_token() {
        return this.oauth2();
    }

    // 处理所有OAuth相关请求
    @RequestMapping("/doConfirm")
    public Object doConfirm() {
        return this.oauth2();
    }

    // 处理所有OAuth相关请求
    @RequestMapping("/doLogin")
    public Object doLogin() {
        return this.oauth2();
    }


    // ---------- 开放相关资源接口： Client端根据 Access-Token ，置换相关资源 ------------
    // 获取Userinfo信息：昵称、头像、性别等等
    @RequestMapping("/userinfo")
    public ResultBody<JbmLoginUser> userinfo() {
        JbmLoginUser jbmLoginUser = null;
        if (StpUtil.isLogin()) {
            jbmLoginUser = LoginHelper.getLoginUser();
        } else {
            // 获取 Access-Token 对应的账号id
            String accessToken = SaHolder.getRequest().getParam("access_token");
            if (StrUtil.isNotBlank(accessToken)) {
                Object loginId = SaOAuth2Util.getLoginIdByAccessToken(accessToken);
                System.out.println("-------- 此Access-Token对应的账号id: " + loginId);
                jbmLoginUser = LoginHelper.getLoginUser(accessToken);
            }
        }
        if (ObjectUtil.isNotEmpty(jbmLoginUser)) {
            return ResultBody.ok().data(jbmLoginUser);
        }
        return ResultBody.error("Token错误,无法获取用户信息");
    }

    @ApiOperation("登出方法")
    @DeleteMapping("logout")
    public ResultBody<Void> logout() {
        try {
            sysLoginService.logout(SaOAuth2Util.getLoginIdByAccessToken(StpUtil.getTokenValue()));
        } catch (NotLoginException e) {
            return ResultBody.failed().msg("还没有登录");
        }
        return ResultBody.ok();
    }


}
