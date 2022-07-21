package com.jbm.cluster.auth.service;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Consts;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.oauth2.model.RequestAuthModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.auth.model.LoginProcessModel;
import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.web.ServletUtils;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class SaOAuthLoginHandle implements BiFunction<String, String, Object> {


    @Override
    public <V> BiFunction<String, String, V> andThen(Function<? super Object, ? extends V> after) {
        return BiFunction.super.andThen(after);
    }

    private SaRequest request;

    @Override
    public Object apply(String username, String password) {
        this.request = SaHolder.getRequest();
        LoginProcessModel loginProcessModel = new LoginProcessModel();
        loginProcessModel.setUsername(username);
        loginProcessModel.setOriginalPassword(password);
        loginProcessModel.setDecryptPassword(password);
        this.preLogin(this.request, loginProcessModel);
        this.preCheck(loginProcessModel);
        this.doCheck(loginProcessModel);
        //生成登录CODE
        RequestAuthModel ra = new RequestAuthModel();
        ra.clientId = loginProcessModel.getClientId();
        ra.responseType = "code";
        ra.redirectUri = request.getUrl();
        ra.state = request.getParam(SaOAuth2Consts.Param.state);
        ra.scope = request.getParam(SaOAuth2Consts.Param.scope, "");
        ra.loginId = StpUtil.getLoginId();
        return ResultBody.ok(SaOAuth2Util.generateCode(ra));
    }

    /**
     * 返回登录类型
     *
     * @return
     */
    public LoginProcessModel preLogin(SaRequest request, LoginProcessModel loginProcessModel) {
        String clientId = request.getParam("client_id");
        if (StrUtil.isBlank(clientId)) {
            throw new SaTokenException("未知应用");
        }
        String vcode = request.getParam("vcode");
        loginProcessModel.setVcode(vcode);
        loginProcessModel.setClientId(clientId);
        String loginTypeValue = request.getParam("loginType");
        loginProcessModel.setLoginTypeValue(loginTypeValue);
        LoginType loginType = LoginType.PASSWORD;
        if (StrUtil.isNotEmpty(loginTypeValue)) {
            loginType = LoginType.valueOf(loginTypeValue.toUpperCase());
            if (loginType == null) {
                loginType = LoginType.PASSWORD;
            }
        }
        if (loginType.equals(LoginType.PASSWORD)) {
            String pwd = doDecryptPassword(loginProcessModel);
            loginProcessModel.setDecryptPassword(pwd);
        }
        loginProcessModel.setLoginType(loginType);
        UserAgent userAgent = UserAgentUtil.parse(ServletUtils.getRequest().getHeader("User-Agent"));
        loginProcessModel.setLoginDevice(userAgent.getOs().getName());
        return loginProcessModel;
    }


    public abstract String doDecryptPassword(LoginProcessModel loginProcessModel);

    public abstract void preCheck(LoginProcessModel loginProcessModel);


    /**
     * 确认身份
     */
    public abstract ResultBody doCheck(LoginProcessModel loginProcessModel);
}
