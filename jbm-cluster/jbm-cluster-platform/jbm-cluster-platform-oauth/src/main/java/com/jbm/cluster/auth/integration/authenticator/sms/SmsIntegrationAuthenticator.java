package com.jbm.cluster.auth.integration.authenticator.sms;

import com.jbm.cluster.api.auth.model.UserAccount;
import com.jbm.cluster.auth.integration.AbstractPreparableIntegrationAuthenticator;
import com.jbm.cluster.auth.integration.IntegrationAuthentication;
import com.jbm.cluster.auth.integration.authenticator.sms.event.SmsAuthenticateBeforeEvent;
import com.jbm.cluster.auth.integration.authenticator.sms.event.SmsAuthenticateSuccessEvent;
import com.jbm.cluster.auth.service.feign.BaseUserServiceClient;
import com.jbm.cluster.auth.service.feign.VerificationCodeClient;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.stereotype.Component;

/**
 * 短信验证码集成认证
 *
 * @author wesley.zhang
 * @date 2018-3-31
 **/
@Component
public class SmsIntegrationAuthenticator extends AbstractPreparableIntegrationAuthenticator implements ApplicationEventPublisherAware {


    @Autowired
    private VerificationCodeClient verificationCodeClient;
    @Autowired
    private BaseUserServiceClient baseUserServiceClient;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ApplicationEventPublisher applicationEventPublisher;

    private final static String SMS_AUTH_TYPE = "mobile";

    @Override
    public UserAccount authenticate(IntegrationAuthentication integrationAuthentication) {
        //获取密码，实际值是验证码
        String password = integrationAuthentication.getAuthParameter("password");
        //获取用户名，实际值是手机号
        String username = integrationAuthentication.getUsername();
        //发布事件，可以监听事件进行自动注册用户
        this.applicationEventPublisher.publishEvent(new SmsAuthenticateBeforeEvent(integrationAuthentication));
        //通过手机号码查询用户
        ResultBody<UserAccount> userAccountResultBody = baseUserServiceClient.userLoginByType(username, SMS_AUTH_TYPE);
        UserAccount userAccount = userAccountResultBody.getResult();
        if (userAccount != null) {
            //将密码设置为验证码
            userAccount.setPassword(passwordEncoder.encode(password));
            //发布事件，可以监听事件进行消息通知
            this.applicationEventPublisher.publishEvent(new SmsAuthenticateSuccessEvent(integrationAuthentication));
        }
        return userAccount;
    }

    @Override
    public void prepare(IntegrationAuthentication integrationAuthentication) {
        String smsToken = integrationAuthentication.getAuthParameter("sms_token");
        String smsCode = integrationAuthentication.getAuthParameter("password");
        String username = integrationAuthentication.getAuthParameter("username");
        ResultBody<Boolean> result = verificationCodeClient.validate(smsToken, smsCode, username);
        if (!result.getResult()) {
            throw new OAuth2Exception("验证码错误或已过期");
        }
    }

    @Override
    public boolean support(IntegrationAuthentication integrationAuthentication) {
        return SMS_AUTH_TYPE.equals(integrationAuthentication.getLoginType());
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
