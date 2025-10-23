package com.jbm.cluster.auth.controller;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.oauth2.exception.SaOAuth2Exception;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Consts;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Handle;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.form.auth.RegisterForm;
import com.jbm.cluster.api.model.auth.AccessTokenResult;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.auth.form.AuthorizeForm;
import com.jbm.cluster.auth.model.ThirdPartyUser;
import com.jbm.cluster.auth.service.ConfirmService;
import com.jbm.cluster.auth.service.SysLoginService;
import com.jbm.cluster.auth.service.ThirdPartyAuthService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

/**
 * @Created wesley.zhang
 * @Date 2022/5/15 10:18
 * @Description TODO
 */
@Slf4j
@Api(tags = "OAuth2认证")
@RestController
@RequestMapping("/oauth2")
public class OAuth2ServerController {

    @Autowired
    private SysLoginService sysLoginService;
    @Autowired
    private ConfirmService confirmService;

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
    @ApiOperation(value = "获取认证token", notes = "")
    @PostMapping("/access_token")
    public Object access_token(HttpSession session, HttpServletResponse response) {
        return ((ResultBody<?>)this.oauth2()).getResult();
    }

    /**
     * 处理所有OAuth相关请求
     *
     * @return
     */
    @ApiOperation(value = "认证", notes = "")
    @GetMapping("/authorize")
    public String authorize(
            @RequestParam(value = "response_type", required = false) String responseType,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "state", required = false) String state,
            Map<String, Object> model) {
        // 将参数传递给模板
        model.put("response_type", responseType);
        model.put("client_id", clientId);
        model.put("redirect_uri", redirectUri);
        model.put("scope", scope);
        model.put("state", state);
        
        // 重定向到HTML授权页面
        return "oauth2_authorize";
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
        Object object = this.oauth2();
        return object;
    }

    @ApiOperation(value = "客户端Token", notes = "")
    @RequestMapping("/client_token")
    public Object client_token() {
        return this.oauth2();
    }

    @ApiOperation(value = "确认认证", notes = "")
    @RequestMapping("/doConfirm")
    public Object doConfirm(@RequestParam(required = false) String code) {
        Object result = this.oauth2();
        confirmService.doConfirm(code);
        return result;
    }

    @ApiOperation(value = "登录", notes = "")
    @PostMapping("/doLogin")
    public Object doLogin(AuthorizeForm authorizeForm) {
        return this.oauth2();
    }




    @ApiOperation("用户注册")
    @PostMapping("/register")
    public ResultBody<Void> register(RegisterForm registerBody) {
        // 用户注册
        sysLoginService.register(registerBody);
        return ResultBody.ok();
    }

    @ApiOperation("续签")
    @PostMapping("/renewal")
    public ResultBody<Void> renewal() {
        // 用户注册
        StpUtil.updateLastActivityToNow();
        return ResultBody.ok();
    }


    // ---------- 开放相关资源接口： Client端根据 Access-Token ，置换相关资源 ------------
    // 获取Userinfo信息：昵称、头像、性别等等
    @SaCheckLogin
    @RequestMapping("/userinfo")
    public ResultBody userinfo() {
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

        return ResultBody.callback(() -> {
            try {
                sysLoginService.logout(null);
            } catch (NotLoginException e) {
                throw new ServiceException("还没有登录");
            }
            return null;
        });
    }


    @Autowired
    private ThirdPartyAuthService thirdPartyAuthService;

    @ApiOperation("第三方登录回调")
    @GetMapping("/thirdparty/{provider}/callback")
    public ResultBody<Object> thirdPartyCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state,
            HttpSession session,
            HttpServletResponse response) throws IOException {

        // 1. 验证 state（防 CSRF）
        String expectedState = (String) session.getAttribute("oauth2_thirdparty_state");
        if (expectedState == null || !expectedState.equals(state)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid state");
            return ResultBody.failed().msg("Invalid state");
        }

        try {
            // 2. 用 code 换取第三方用户信息
            ThirdPartyUser thirdUser = thirdPartyAuthService.getUserInfoByCode(code,provider);
            // 3. 将第三方用户映射为你系统内的用户（自动注册或关联）
            ResultBody<JbmLoginUser> jbmLoginUserResultBody = sysLoginService.login(thirdUser.getSubjectId(), code, LoginType.THIRD_PARTY);
            JbmLoginUser myUser = jbmLoginUserResultBody.getResult();

            AccessTokenResult accessTokenResult = new AccessTokenResult();
            accessTokenResult.setAccessToken(myUser.getToken());
            accessTokenResult.setExpiresIn(myUser.getExpireTime() - System.currentTimeMillis());
            accessTokenResult.setScope("*");
            return ResultBody.ok().data(accessTokenResult);

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login failed");
            return ResultBody.failed().msg("第三方登录失败Third-party OAuth2 login failed");

        }
    }


}