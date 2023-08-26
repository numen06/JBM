package com.jbm.cluster.auth.integration.authenticator;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.form.ThirdPartyUserForm;
import com.jbm.cluster.api.model.UserAccount;
import com.jbm.cluster.auth.integration.AbstractPreparableIntegrationAuthenticator;
import com.jbm.cluster.auth.integration.IntegrationAuthentication;
import com.jbm.cluster.auth.service.feign.BaseUserServiceClient;
import com.jbm.cluster.auth.service.feign.WeixinClient;
import com.jbm.framework.metadata.bean.ResultBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 微信认证
 *
 * @author wesley.zhang
 * @date 2018-3-31
 **/
@Slf4j
@Component
public class WeixinIntegrationAuthenticator extends AbstractPreparableIntegrationAuthenticator {

    private final static String WEIXIN_CODE_AUTH_TYPE = "weixin";
    @Autowired
    private BaseUserServiceClient baseUserServiceClient;
    @Autowired(required = false)
    private WeixinClient weixinClient;

    @Autowired(required = false)
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public UserAccount authenticate(IntegrationAuthentication integrationAuthentication) {
        //获取密码，实际值是OPENID
        String password = integrationAuthentication.getAuthParameter("password");
        //获取用户名，实际值是手机号
        String username = integrationAuthentication.getUsername();
        //手机号
        String phone = integrationAuthentication.getAuthParameter("phone");
        //发布事件，可以监听事件进行自动注册用户
        UserAccount userAccount = null;
        //通过手机号码查询用户
        ResultBody<UserAccount> userAccountResultBody = baseUserServiceClient.userLoginByType(username, WEIXIN_CODE_AUTH_TYPE);
        userAccount = userAccountResultBody.getResult();
        if (userAccount != null) {
            //将密码设置为验证码
            userAccount.setPassword(passwordEncoder.encode(password));
            //发布事件，可以监听事件进行消息通知
        } else if (StrUtil.isNotBlank(phone)) {
//            if (StrUtil.isNotBlank(phone)) {
//                baseUserServiceClient.bindUserThirdPartyByPhone(username, passwordEncoder.encode(password), WEIXIN_CODE_AUTH_TYPE, phone);
//                //再次获取用户
//                userAccountResultBody = baseUserServiceClient.userLoginByType(username, WEIXIN_CODE_AUTH_TYPE);
//                if (ObjectUtil.isEmpty(userAccountResultBody)) {
//                    throw new OAuth2Exception("系统不识别当前用户");
//                }
//                userAccount = userAccountResultBody.getResult();
//                userAccount.setPassword(passwordEncoder.encode(password));
//            }
            ThirdPartyUserForm thirdPartyUserForm = new ThirdPartyUserForm();
            thirdPartyUserForm.setAccount(username);
            thirdPartyUserForm.setPassword(password);
            thirdPartyUserForm.setPhone(phone);
            thirdPartyUserForm.setAccountType(WEIXIN_CODE_AUTH_TYPE);
            userAccountResultBody = baseUserServiceClient.loginAndRegisterMobileUser(thirdPartyUserForm);
            userAccount = userAccountResultBody.getResult();
        }
        return userAccount;
    }

    @Override
    public void prepare(IntegrationAuthentication integrationAuthentication) {
//        String username = integrationAuthentication.getUsername();
//        String phone = integrationAuthentication.getAuthParameter("phone");
//        ResultBody<UserAccount> userAccountResultBody = baseUserServiceClient.userLoginByType(username, WEIXIN_CODE_AUTH_TYPE);
//        if (result.getSuccess()) {
//            log.info("获取到微信信息:{}", result.getResult());
//            JSONObject jsonObject = JSON.parseObject(result.getResult());
//        } else {
//            throw new OAuth2Exception(result.getMessage());
//        }
    }

    @Override
    public boolean support(IntegrationAuthentication integrationAuthentication) {
        return WEIXIN_CODE_AUTH_TYPE.equals(integrationAuthentication.getLoginType());
    }
}
