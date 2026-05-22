package com.jbm.cluster.auth.service;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.oauth2.config.SaOAuth2Config;
import cn.dev33.satoken.oauth2.exception.SaOAuth2Exception;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Consts;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.constants.RequestDeviceType;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.auth.RegisterForm;
import com.jbm.cluster.api.model.auth.AccessTokenResult;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.cluster.auth.business.AuthUserBusiness;
import com.jbm.cluster.auth.model.LoginProcessModel;
import com.jbm.cluster.api.form.user.ThirdPartyUser;
import com.jbm.cluster.common.mysql.event.LoginDatabaseHook;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.web.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class SysLoginService {

    @Autowired
    private BaseAppPreprocessing baseAppPreprocessing;
    @Autowired
    private VCoderService vCoderService;
    @Autowired
    private DynamicLoginFeignClient dynamicLoginFeignClient;
    @Autowired
    private LoginPostProcessor loginPostProcessor;
    @Autowired
    private LoginLifecyclePublisher loginLifecyclePublisher;
    @Autowired
    private AuthUserBusiness authUserBusiness;
    @Autowired
    private LoginDatabaseHook loginDatabaseHook;
    @Autowired
    private UserService userService;

    public JbmLoginUser conventJbmLoginUser(BaseUser baseUser) {
        JbmLoginUser jbmLoginUser = new JbmLoginUser();
        BeanUtil.copyProperties(baseUser, jbmLoginUser);
        return jbmLoginUser;
    }

    @Autowired
    public void setSaOAuth2Config(SaOAuth2Config cfg) {
        cfg.setNotLoginView(() -> ResultBody.error("你还没有登录"))
                .setDoLoginHandle(new SaOAuthLoginHandler() {
                    @Override
                    public String doDecryptPassword(LoginProcessModel loginProcessModel) {
                        if (LoginType.MINIAPP.equals(loginProcessModel.getLoginType())
                                || LoginType.WECHAT.equals(loginProcessModel.getLoginType())) {
                            return loginProcessModel.getOriginalPassword();
                        }
                        return decryptPassword(loginProcessModel.getClientId(), loginProcessModel.getOriginalPassword());
                    }

                    @Override
                    public void preCheck(LoginProcessModel loginProcessModel) {
                        List<LoginType> verifyTypes = Lists.newArrayList(LoginType.PASSWORD, LoginType.SMS);
                        if (verifyTypes.contains(loginProcessModel.getLoginType())
                                && StrUtil.isNotBlank(loginProcessModel.getVcode())) {
                            vCoderService.verify(loginProcessModel.getVcode(), null);
                        }
                        loginDatabaseHook.assertNotLocked(loginProcessModel.getUsername(),
                                loginProcessModel.getLoginType() != null ? loginProcessModel.getLoginType().name() : null);
                        SaOAuth2Util.checkClientModel(loginProcessModel.getClientId());
                    }

                    @Override
                    public ResultBody doCheck(LoginProcessModel loginProcessModel) {
                        return checkLoginIdentity(loginProcessModel);
                    }

                    @Override
                    protected void onAuthFailure(LoginProcessModel loginProcessModel, String reason) {
                        loginLifecyclePublisher.publishLoginFailure(loginProcessModel, reason);
                    }
                })
                .setConfirmView((clientId, scope) -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("clientId", clientId);
                    map.put("scope", scope);
                    return ResultBody.error("确认登录");
                });
    }

    public ResultBody checkLoginIdentity(LoginProcessModel loginProcessModel) {
        ResultBody<JbmLoginUser> resultBody;
        resultBody = this.login(loginProcessModel.getUsername(), loginProcessModel.getDecryptPassword(),
                loginProcessModel.getLoginType());
        if (resultBody.getSuccess()) {
            log.info("获取到了用户信息,触发登录");
            JbmLoginUser jbmLoginUser = resultBody.getResult();
            loginPostProcessor.enrichLoginUser(jbmLoginUser, loginProcessModel.getClientId());
            jbmLoginUser.setDevice(loginProcessModel.getLoginDevice());
            LoginHelper.login(jbmLoginUser,
                    loginProcessModel.getLoginType() != null ? loginProcessModel.getLoginType().name() : null,
                    loginProcessModel.getClientId(),
                    LoginLifecyclePublisher.currentIp(),
                    LoginLifecyclePublisher.currentUserAgent());
        } else {
            throw new SaOAuth2Exception(StrUtil.emptyToDefault(resultBody.getMessage(), "登录验证失败"));
        }
        return resultBody;
    }

    public String decryptPassword(String clientId, String key) {
        if (Objects.equals(clientId, "demo")) {
            return key;
        }
        try {
            BaseApp baseApp = baseAppPreprocessing.getAppByKey(clientId);
            if (ObjectUtil.hasNull(baseApp.getPrivateKey(), baseApp.getPublicKey())) {
                throw new ServiceException("公钥私钥可能存在为空");
            }
            RSA rsa = SecureUtil.rsa(baseApp.getPrivateKey(), baseApp.getPublicKey());
            return rsa.decryptStr(key, KeyType.PrivateKey);
        } catch (Exception e) {
            log.error("解密错误", e);
            throw new ServiceException("处理登录信息异常");
        }
    }

    public ResultBody<JbmLoginUser> login(String username, String password, LoginType loginType) {
        ILoginAuthenticate loginAuthenticate = dynamicLoginFeignClient.getFeginLoginAuthenticate(loginType);
        return loginAuthenticate.login(username, password, loginType.toString());
    }

    public ResultBody<JbmLoginUser> thirdPartyLogin(ThirdPartyUser thirdPartyUser) {
        ILoginAuthenticate loginAuthenticate = dynamicLoginFeignClient.getFeginLoginAuthenticate(LoginType.THIRD_PARTY);
        String json = JSON.toJSONString(thirdPartyUser);
        return loginAuthenticate.login(json, null, LoginType.THIRD_PARTY.toString());
    }

    public AccessTokenResult login(String username, String password) {
        UserAccount userAccount = authUserBusiness.login(username);
        if (userAccount == null) {
            loginLifecyclePublisher.publishLoginFailure(username, LoginType.PASSWORD.name(), null, "用户不存在");
            throw new ServiceException("用户不存在");
        }
        if (!SecurityUtils.getPasswordEncoder().matches(password, userAccount.getPassword())) {
            loginLifecyclePublisher.publishLoginFailure(username, LoginType.PASSWORD.name(), null, "密码错误");
            throw new ServiceException("密码错误");
        }
        JbmLoginUser loginUser = userService.userAccountToLoginUser(userAccount);
        LoginHelper.loginByDevice(loginUser, RequestDeviceType.PC.getDevice());
        return accessTokenResult(StpUtil.getTokenInfo());
    }

    private AccessTokenResult accessTokenResult(SaTokenInfo tokenInfo) {
        AccessTokenResult accessTokenResult = new AccessTokenResult();
        accessTokenResult.setAccessToken(tokenInfo.getTokenValue());
        accessTokenResult.setExpiresIn(tokenInfo.getTokenActivityTimeout());
        return accessTokenResult;
    }

    public void logout(Object loginId) {
        if (loginId != null) {
            LoginHelper.loginout(loginId);
        } else {
            LoginHelper.loginout();
        }
    }

    public void register(RegisterForm registerBody) {
        vCoderService.verify(registerBody.getVcode());
        BaseUser sysUser = new BaseUser();
        sysUser.setUserName(registerBody.getUserName());
        sysUser.setNickName(StrUtil.isBlank(registerBody.getNickName()) ? registerBody.getUserName() : registerBody.getNickName());
        sysUser.setUserType(JbmConstants.USER_TYPE_NORMAL);
        String originalPassword = registerBody.getPassword();
        if (StrUtil.isNotBlank(originalPassword)) {
            SaRequest req = SaHolder.getRequest();
            String clientId = req.getParamNotNull(SaOAuth2Consts.Param.client_id);
            sysUser.setPassword(decryptPassword(clientId, originalPassword));
        }
        HttpServletRequest request = WebUtils.getHttpServletRequest();
        String registerIp = request != null ? request.getRemoteAddr() : null;
        authUserBusiness.register(sysUser, registerIp);
    }
}
