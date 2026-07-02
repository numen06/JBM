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
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.constants.RequestDeviceType;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.form.auth.RegisterForm;
import com.jbm.cluster.api.form.user.ThirdPartyUser;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.auth.form.AuthorizeForm;
import com.jbm.cluster.auth.model.LoginProcessModel;
import com.jbm.cluster.auth.service.BaseAppPreprocessing;
import com.jbm.cluster.auth.service.ConfirmService;
import com.jbm.cluster.auth.service.SysLoginService;
import com.jbm.cluster.auth.service.ThirdPartyAuthService;
import com.jbm.cluster.common.basic.service.LoginErrorMessageService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.metadata.enumerate.ErrorCode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
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
    @Autowired
    private BaseAppPreprocessing baseAppPreprocessing;
    @Autowired
    private LoginErrorMessageService loginErrorMessageService;

    // 处理所有OAuth相关请求
    public Object oauth2() {
        System.out.println("------- 进入请求: " + SaHolder.getRequest().getUrl());
        Object result = SaOAuth2Handle.serverRequest();
        if (SaOAuth2Consts.NOT_HANDLE.equals(result)) {
            return ResultBody.failed().httpStatus(400).code(400).msg("输入参数错误,没有找到匹配的授权模式");
        }

        if (result instanceof SaResult) {
            SaRequest req = SaHolder.getRequest();
            if (req.isParam(SaOAuth2Consts.Param.response_type, SaOAuth2Consts.ResponseType.token)) {
                return ((SaResult) result).getData();
            }
            SaResult saResult = (SaResult) result;
            if (SaResult.CODE_SUCCESS == saResult.getCode()) {
                req = SaHolder.getRequest();
                if (req.isPath(SaOAuth2Consts.Api.token)) {
                    if (saResult.getData() instanceof Map) {
                        Map<String, Object> data = (Map<String, Object>) saResult.getData();
                        data.put("token_type", SaManager.getConfig().getTokenPrefix());
                        // 确保 access_token 是 Sa-Token 的 token
                        if (data.containsKey("access_token") && StpUtil.isLogin()) {
                            String currentToken = StpUtil.getTokenValue();
                            if (StrUtil.isNotBlank(currentToken)) {
                                data.put("access_token", currentToken);
                                log.debug("统一 access_token 为 Sa-Token token: {}", currentToken);
                            }
                        }
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
            LoginProcessModel loginProcessModel = new LoginProcessModel();
            loginProcessModel.setUsername(authorizeForm.getUsername());
            loginProcessModel.setOriginalPassword(authorizeForm.getPassword());
            loginProcessModel.setDecryptPassword(authorizeForm.getPassword());
            loginProcessModel.setLoginType(LoginType.PASSWORD);
            loginProcessModel.setClientId(authorizeForm.getClient_id());
            loginProcessModel.setLoginDevice(RequestDeviceType.PC.getDevice());

            ResultBody<JbmLoginUser> loginResult = sysLoginService.checkLoginIdentity(loginProcessModel);
            JbmLoginUser jbmLoginUser = loginResult.getResult();

            RequestAuthModel ra = new RequestAuthModel();
            ra.clientId = authorizeForm.getClient_id();
            ra.responseType = authorizeForm.getResponse_type();
            ra.redirectUri = authorizeForm.getRedirect_uri();
            ra.state = authorizeForm.getState();
            ra.scope = StrUtil.isNotBlank(authorizeForm.getScope()) ? authorizeForm.getScope() : "";
            ra.loginId = jbmLoginUser.getLoginId();

            Object codeModel = SaOAuth2Util.generateCode(ra);
            String code = String.valueOf(codeModel);

            if (codeModel != null && codeModel.getClass().getName().contains("CodeModel")) {
                try {
                    code = (String) codeModel.getClass().getMethod("getCode").invoke(codeModel);
                } catch (Exception e) {
                    code = codeModel.toString();
                }
            }

            String callbackUrl = SaOAuth2Util.buildRedirectUri(
                    authorizeForm.getRedirect_uri(),
                    code,
                    authorizeForm.getState()
            );

            log.info("OAuth2 登录成功，用户: {}, 授权码已生成，回调地址: {}", authorizeForm.getUsername(), callbackUrl);
            ResultBody<String> response = ResultBody.ok(callbackUrl);
            if (loginResult.getExtra() != null) {
                loginResult.getExtra().forEach(response::put);
            }
            if (StrUtil.isNotBlank(loginResult.getMessage())
                    && !ErrorCode.OK.getMessage().equals(loginResult.getMessage())) {
                response.msg(loginResult.getMessage());
            } else {
                response.msg("登录成功");
            }
            return response;
        } catch (Exception e) {
            log.error("OAuth2 登录失败", e);
            return ResultBody.<String>failed().msg(loginErrorMessageService.resolve(e.getMessage()));
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
        try {
            // 检查当前登录状态
            if (StpUtil.isLogin()) {
                // 已登录，直接续期当前 token
                StpUtil.updateLastActivityToNow();
                log.info("续签成功：当前token={}, 过期时间={}", 
                        StpUtil.getTokenValue(), StpUtil.getTokenInfo().getTokenActivityTimeout());
            } else {
                // 未登录，尝试通过 access_token 参数获取
                SaRequest req = SaHolder.getRequest();
                String accessToken = req.getParam("access_token");
                if (StrUtil.isNotBlank(accessToken)) {
                    try {
                        Object loginId = SaOAuth2Util.getLoginIdByAccessToken(accessToken);
                        if (loginId != null) {
                            // 通过 loginId 续期对应的 token
                            String tokenValue = StpUtil.getTokenValueByLoginId(loginId);
                            if (StrUtil.isNotBlank(tokenValue)) {
                                // 使用 token 值来续期
                                StpUtil.updateLastActivityToNow();
                                log.info("续签成功：通过access_token续期，loginId={}, token={}, 过期时间={}", 
                                        loginId, tokenValue, StpUtil.getTokenInfo().getTokenActivityTimeout());
                            } else {
                                return ResultBody.<Void>failed().msg("无法找到对应的token");
                            }
                        } else {
                            return ResultBody.<Void>failed().msg("无效的access_token");
                        }
                    } catch (Exception e) {
                        log.error("续签失败", e);
                        return ResultBody.<Void>failed().msg("续签失败：" + e.getMessage());
                    }
                } else {
                    return ResultBody.<Void>failed().msg("未登录且未提供access_token");
                }
            }
            return ResultBody.ok();
        } catch (Exception e) {
            log.error("续签异常", e);
            return ResultBody.<Void>failed().msg("续签失败：" + e.getMessage());
        }
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
            return ResultBody.<JbmLoginUser>ok().data(jbmLoginUser);
        }
        return ResultBody.<JbmLoginUser>error("Token错误,无法获取用户信息");
    }

    @ApiOperation("登出方法")
    @DeleteMapping("logout")
    public ResultBody<Void> logout(HttpServletResponse response) {
        return ResultBody.callback(() -> {
            try {
                //如果登录类型是第三方登录则调用第三方登录的退出
                JbmLoginUser loginUser = LoginHelper.getLoginUser();
                log.info("获取到用户信息:{}", loginUser);
                sysLoginService.logout(null);
                try {
                    String loginUrl = thirdPartyAuthService.logout(loginUser.getAccountType(), loginUser);
                    //浏览器重定向
                    if (loginUrl != null) {
//                        response.setStatus(302);
//                        response.setHeader("Location", loginUrl);
                    }
//                    return loginUrl;
                } catch (Exception e) {
                    log.error("第三方登录退出失败", e);
                }
                return null;
            } catch (NotLoginException e) {
                throw new ServiceException("还没有登录");
            }
        });
    }

    @ApiOperation("登录回调")
    @GetMapping("/callback")
    public Object callback(
            @RequestParam String code,
            @RequestParam(required = false) String state) throws IOException {
        //在request中默认设置参数设置为code模式
        log.info("登录回调，code: {}, state: {}", code, state);
        // 获取变量
        SaResponse res = SaHolder.getResponse();
        // 获取参数
        CodeModel codeModel = SaOAuth2Util.getCode(code);
        if (codeModel == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return ResultBody.failed().msg("code参数错误");
        }
        // 构建 Access-Token
        AccessTokenModel token = SaOAuth2Util.generateAccessToken(code);
        // 确保 access_token 是 Sa-Token 的 token
        if (token != null && StpUtil.isLogin()) {
            String currentToken = StpUtil.getTokenValue();
            if (StrUtil.isNotBlank(currentToken)) {
                token.accessToken = currentToken;
                log.debug("统一 access_token 为 Sa-Token token: {}", currentToken);
            }
        }
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
            @RequestParam(value = "client_id", required = false) String targetClientId,
            @RequestParam(value = "client_secret", required = false) String targetClientSecret,
            @RequestParam(required = false) String state,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            HttpServletResponse response) throws IOException {

        log.info("========== 第三方登录回调开始 ==========");
        log.info("[第三方回调] 提供商: {}", provider);
        log.info("[第三方回调] 授权码: {}", code);
        log.info("[第三方回调] 状态码: {}", state);
        log.info("[第三方回调] 回调地址: {}", redirectUri);

        // 1. 验证 state（防 CSRF）
//        String expectedState = (String) session.getAttribute("oauth2_thirdparty_state");
//        if (expectedState == null || !expectedState.equals(state)) {
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid state");
//            return ResultBody.failed().msg("Invalid state");
//        }

        try {
            // 2. 用 code 换取第三方用户信息
            log.info("[第三方回调] Step 1: 开始获取第三方用户信息...");
            ThirdPartyUser thirdUser = thirdPartyAuthService.getUserInfoByCode(code, redirectUri, provider);

            if (thirdUser == null) {
                log.error("[第三方回调] Step 1: 获取第三方用户信息失败，返回null");
                return ResultBody.failed().msg("获取第三方用户信息失败");
            }

            log.info("[第三方回调] Step 1: 获取第三方用户信息成功");
            log.info("[第三方回调] 第三方用户ID: {}", thirdUser.getSubjectId());
            log.info("[第三方回调] 用户名: {}", thirdUser.getUsername());
            log.info("[第三方回调] 昵称: {}", thirdUser.getNickname());
            log.info("[第三方回调] 邮箱: {}", thirdUser.getEmail());
            log.info("[第三方回调] 手机: {}", thirdUser.getMobile());

            // 3. 将第三方用户映射为你系统内的用户（自动注册或关联）
            log.info("[第三方回调] Step 2: 开始映射/注册系统用户...");
            ResultBody<JbmLoginUser> jbmLoginUserResultBody = sysLoginService.thirdPartyLogin(thirdUser);

            if (!jbmLoginUserResultBody.getSuccess()) {
                log.error("[第三方回调] Step 2: 系统用户映射失败: {}", jbmLoginUserResultBody.getMessage());
                return ResultBody.failed().msg("用户映射失败: " + jbmLoginUserResultBody.getMessage());
            }

            //通过oauth登录
            JbmLoginUser myUser = jbmLoginUserResultBody.getResult();
            if (StrUtil.isEmpty(myUser.getClientId())) {
                log.info("[第三方回调] 映射用户没有clientId,设置clientId");
                if (StrUtil.isEmpty(targetClientId)) {
                    myUser.setClientId("g6LLZlu9nv0bRz73eHaxrMJQ");
                } else {
                    myUser.setClientId(targetClientId);
                }
            }

            // 设置 AppId (通过 clientId 获取)
            BaseApp baseApp = baseAppPreprocessing.getAppByKey(myUser.getClientId());
            myUser.setAppId(baseApp.getAppId());
            // 设置设备类型
            if (StrUtil.isBlank(myUser.getDevice())) {
                myUser.setDevice(RequestDeviceType.PC.getDevice());
            }

            log.info("[第三方回调] Step 2: 系统用户映射成功");
            log.info("[第三方回调] 系统用户ID: {}", myUser.getLoginId());
            log.info("[第三方回调] 系统用户名: {}", myUser.getUsername());
            log.info("[第三方回调] 系统菜单权限数量: {}", CollUtil.size(myUser.getMenuPermission()));

            // 4. 执行登录
            log.info("[第三方回调] Step 3: 开始执行用户登录...");
            RequestAuthModel requestAuthModel = new RequestAuthModel();
            requestAuthModel.setLoginId(myUser.getLoginId());
            requestAuthModel.setClientId(myUser.getClientId());
            requestAuthModel.setScope("all");
            LoginHelper.login(myUser);
            String currentToken = StpUtil.getTokenValue();
            log.info("[第三方回调] 登录状态:{}，当前用户token:{}", StpUtil.isLogin(), currentToken);
            AccessTokenModel accessTokenResult = SaOAuth2Util.generateAccessToken(requestAuthModel, true);
            // 确保 access_token 是 Sa-Token 的 token
            if (accessTokenResult != null && StrUtil.isNotBlank(currentToken)) {
                accessTokenResult.accessToken = currentToken;
                log.info("[第三方回调] 统一 access_token 为 Sa-Token token: {}", currentToken);
            }
            log.info("[第三方回调] Token: {}", myUser.getToken());
            log.info("[第三方回调] Step 3: 用户登录成功");
            // 5. 获取AccessToken
            log.info("[第三方回调] Step 4: 获取AccessToken...");
            //获取token对象
//            if (accessTokenResult == null) {
//                 log.error("[第三方回调] 通过token获取实体失败");
//                 //瓶装一个AccessTokenModel返回
//                 accessTokenResult = new AccessTokenModel(myUser.getToken(), myUser.getClientId(), myUser.getLoginId(), "all");
//            }
            log.info("[第三方回调] Step 4: AccessToken获取成功");
            log.info("[第三方回调] AccessToken详情: {}", accessTokenResult);
            if (accessTokenResult == null) {
                log.error("[第三方回调] AccessToken获取失败，返回null");
                return ResultBody.failed().msg("AccessToken获取失败");
            }
            myUser.setToken(accessTokenResult.accessToken);
            //如果设置了跳转则跳转
            if (StrUtil.isNotEmpty(redirectUri)) {
                log.info("[第三方回调] Step 5: 执行重定向到: {}", redirectUri);
                response.sendRedirect(redirectUri);
                log.info("========== 第三方登录回调成功（重定向模式） ==========");
                return ResultBody.ok();
            }

            log.info("[第三方回调] Step 5: 返回Token信息（JSON模式）");
            log.info("========== 第三方登录回调成功 ==========");
            return ResultBody.ok().data(accessTokenResult.toLineMap());
        } catch (Exception e) {
            log.error("========== 第三方登录回调失败 ==========");
            log.error("[第三方回调] 异常类型: {}", e.getClass().getName());
            log.error("[第三方回调] 异常信息: {}", e.getMessage());
            log.error("[第三方回调] 详细堆栈信息:", e);
//            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login failed");
            return ResultBody.failed().msg(loginErrorMessageService.resolve(e.getMessage())).exception(e);

        }
    }


}