package com.jbm.cluster.auth.integration.authenticator;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.model.UserAccount;
import com.jbm.cluster.auth.integration.AbstractPreparableIntegrationAuthenticator;
import com.jbm.cluster.auth.integration.IntegrationAuthentication;
import com.jbm.cluster.auth.integration.authenticator.sms.event.SmsAuthenticateBeforeEvent;
import com.jbm.cluster.auth.integration.authenticator.sms.event.SmsAuthenticateSuccessEvent;
import com.jbm.cluster.auth.service.feign.BaseUserServiceClient;
import com.jbm.cluster.auth.service.feign.WeixinClient;
import com.jbm.framework.metadata.bean.ResultBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public UserAccount authenticate(IntegrationAuthentication integrationAuthentication) {
        //获取密码，实际值是OPENID
        String password = integrationAuthentication.getAuthParameter("password");
        //获取用户名，实际值是手机号
        String username = integrationAuthentication.getUsername();
        //发布事件，可以监听事件进行自动注册用户
        UserAccount userAccount = null;
        //通过手机号码查询用户
        ResultBody<UserAccount> userAccountResultBody = baseUserServiceClient.userLoginByType(username, WEIXIN_CODE_AUTH_TYPE);
        userAccount = userAccountResultBody.getResult();
        if (userAccount != null) {
            //将密码设置为验证码
            userAccount.setPassword(passwordEncoder.encode(password));
            //发布事件，可以监听事件进行消息通知
        } else {
            String phone = integrationAuthentication.getAuthParameter("phone");
            userAccount.setPassword(passwordEncoder.encode(password));
            baseUserServiceClient.bindUserThirdPartyByPhone(username, password, WEIXIN_CODE_AUTH_TYPE, phone);
        }
        //再次获取用户
        userAccountResultBody = baseUserServiceClient.userLoginByType(username, WEIXIN_CODE_AUTH_TYPE);
        userAccount = userAccountResultBody.getResult();
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
