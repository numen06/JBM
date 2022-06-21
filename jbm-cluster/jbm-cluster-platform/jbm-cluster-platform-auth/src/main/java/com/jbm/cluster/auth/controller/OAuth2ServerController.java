package com.jbm.cluster.auth.controller;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Consts;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Handle;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.form.auth.RegisterForm;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.auth.form.AuthorizeForm;
import com.jbm.cluster.auth.service.SysLoginService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @Created wesley.zhang
 * @Date 2022/5/15 10:18
 * @Description TODO
 */
@Api(tags = "OAuth2认证")
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
                SaRequest req = SaHolder.getRequest();
                if (req.isPath(SaOAuth2Consts.Api.token)) {
                    if (saResult.getData() instanceof Map) {
                        Map<String, Object> data = (Map<String, Object>) saResult.getData();
                        data.put("token_type", SaManager.getConfig().getTokenPrefix());
                    }
                }
                return ResultBody.ok().data(saResult.getData());
            } else {
                return ResultBody.failed().data(saResult.getData()).msg(saResult.getMsg());
            }
        }
        return result;
    }

    /**
     * 处理所有OAuth相关请求
     *
     * @return
     */
    @ApiOperation(value = "认证", notes = "")
    @RequestMapping("/authorize")
    public Object authorize(AuthorizeForm authorizeForm) {
        return this.oauth2();
    }

    /**
     * 处理所有OAuth相关请求
     *
     * @return
     */
    @ApiOperation(value = "获取token", notes = "")
    @RequestMapping("/token")
    public Object token(AuthorizeForm authorizeForm) {
        return this.oauth2();
    }

    @ApiOperation(value = "刷新token", notes = "")
    @RequestMapping("/refresh")
    public Object refresh() {
        return this.oauth2();
    }

    @ApiOperation(value = "客户端Token", notes = "")
    @RequestMapping("/client_token")
    public Object client_token() {
        return this.oauth2();
    }

    @ApiOperation(value = "确认认证", notes = "")
    @RequestMapping("/doConfirm")
    public Object doConfirm() {
        return this.oauth2();
    }

    @ApiOperation(value = "登录", notes = "")
    @RequestMapping("/doLogin")
    public Object doLogin() {
        return this.oauth2();
    }


    @ApiOperation("用户注册")
    @PostMapping("register")
    public ResultBody<Void> register(@RequestBody RegisterForm registerBody) {
        // 用户注册
        sysLoginService.register(registerBody);
        return ResultBody.ok();
    }

    // ---------- 开放相关资源接口： Client端根据 Access-Token ，置换相关资源 ------------
    // 获取Userinfo信息：昵称、头像、性别等等
    @SaCheckLogin
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
            sysLoginService.logout(null);
        } catch (NotLoginException e) {
            return ResultBody.failed().msg("还没有登录");
        }
        return ResultBody.ok();
    }

}
