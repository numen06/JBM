package com.jbm.cluster.auth.controller;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Consts;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Handle;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.oauth2.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.model.CodeModel;
import cn.dev33.satoken.oauth2.model.RequestAuthModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.form.auth.RegisterForm;
import com.jbm.cluster.api.form.user.ThirdPartyUser;
import com.jbm.cluster.api.model.auth.AccessTokenResult;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.auth.form.AuthorizeForm;
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
        return ((ResultBody<?>) this.oauth2()).getResult();
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
    public ResultBody<?> doLogin(AuthorizeForm authorizeForm) {
        try {
            // 先进行用户登录
            ResultBody<JbmLoginUser> loginResult = sysLoginService.login(
                    authorizeForm.getUsername(),
                    authorizeForm.getPassword(),
                    LoginType.PASSWORD
            );

            if (!loginResult.getSuccess()) {
                return ResultBody.<String>failed().msg(loginResult.getMessage());
            }

            LoginHelper.login(loginResult.getResult());

            // 登录成功后，直接生成授权码
            RequestAuthModel ra = new RequestAuthModel();
            ra.clientId = authorizeForm.getClient_id();
            ra.responseType = authorizeForm.getResponse_type();
            ra.redirectUri = authorizeForm.getRedirect_uri();
            ra.state = authorizeForm.getState();
            ra.scope = StrUtil.isNotBlank(authorizeForm.getScope()) ? authorizeForm.getScope() : "";
            ra.loginId = loginResult.getResult().getLoginId();


            // 生成授权码
            Object codeModel = SaOAuth2Util.generateCode(ra);
            String code = String.valueOf(codeModel);

            // 如果 codeModel 有 code 属性，尝试获取
            if (codeModel != null && codeModel.getClass().getName().contains("CodeModel")) {
                try {
                    code = (String) codeModel.getClass().getMethod("getCode").invoke(codeModel);
                } catch (Exception e) {
                    // 如果获取失败，使用 toString
                    code = codeModel.toString();
                }
            }

            // 构建回调 URL
            String callbackUrl = SaOAuth2Util.buildRedirectUri(
                    authorizeForm.getRedirect_uri(),
                    code,
                    authorizeForm.getState()
            );

            log.info("OAuth2 登录成功，用户: {}, 授权码已生成，回调地址: {}", authorizeForm.getUsername(), callbackUrl);
            return ResultBody.<String>ok(callbackUrl).msg("登录成功");
        } catch (Exception e) {
            log.error("OAuth2 登录失败", e);
            return ResultBody.<String>failed().msg("登录失败：" + e.getMessage());
        }
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

    @ApiOperation("第三方登录回调")
    @GetMapping("/callback")
    public Object callback(
            @RequestParam String code,
            @RequestParam(required = false) String state) throws IOException {
        //在request中默认设置参数设置为code模式
        log.info("第三方登录回调，code: {}, state: {}", code, state);
        // 获取变量
        SaRequest req = SaHolder.getRequest();
        SaResponse res = SaHolder.getResponse();
        // 获取参数
        CodeModel codeModel = SaOAuth2Util.getCode(code);
        if (codeModel == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ResultBody.failed().msg("code参数错误");
        }
        // 构建 Access-Token
        AccessTokenModel token = SaOAuth2Util.generateAccessToken(code);
        // 返回
        return ResultBody.ok(token);
    }


    @Autowired
    private ThirdPartyAuthService thirdPartyAuthService;

    @ApiOperation("第三方登录回调")
    @GetMapping("/thirdparty/{provider}/callback")
    public ResultBody<Object> thirdPartyCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            HttpServletResponse response) throws IOException {

        // 1. 验证 state（防 CSRF）
//        String expectedState = (String) session.getAttribute("oauth2_thirdparty_state");
//        if (expectedState == null || !expectedState.equals(state)) {
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid state");
//            return ResultBody.failed().msg("Invalid state");
//        }

        try {
            // 2. 用 code 换取第三方用户信息
            ThirdPartyUser thirdUser = thirdPartyAuthService.getUserInfoByCode(code, provider);
            if (thirdUser == null) {
                return ResultBody.failed().msg("获取第三方用户信息失败");
            }
            // 3. 将第三方用户映射为你系统内的用户（自动注册或关联）
            ResultBody<JbmLoginUser> jbmLoginUserResultBody = sysLoginService.thirdPartyLogin(thirdUser);
            JbmLoginUser myUser = jbmLoginUserResultBody.getResult();
            LoginHelper.login(myUser);
            AccessTokenModel accessTokenResult = SaOAuth2Util.getAccessToken(myUser.getToken());
            //如果设置了跳转则跳转
            if (StrUtil.isNotEmpty(redirectUri)) {
                response.sendRedirect(redirectUri);
                return ResultBody.ok();
            }
            return ResultBody.ok().data(accessTokenResult.toLineMap());
        } catch (Exception e) {
//            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login failed");
            return ResultBody.failed().msg("第三方登录失败Third-party OAuth2 login failed");

        }
    }


}