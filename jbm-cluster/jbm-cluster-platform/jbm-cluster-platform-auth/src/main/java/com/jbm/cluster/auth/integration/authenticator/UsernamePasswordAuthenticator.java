package com.jbm.cluster.auth.integration.authenticator;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.model.UserAccount;
import com.jbm.cluster.auth.integration.AbstractPreparableIntegrationAuthenticator;
import com.jbm.cluster.auth.integration.IntegrationAuthentication;
import com.jbm.cluster.auth.service.feign.BaseUserServiceClient;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 默认登录处理
 *
 * @author wesley.zhang
 * @date 2018-3-31
 **/
@Component
@Primary
public class UsernamePasswordAuthenticator extends AbstractPreparableIntegrationAuthenticator {

    @Autowired
    private BaseUserServiceClient baseUserServiceClient;

    @Override
    public UserAccount authenticate(IntegrationAuthentication integrationAuthentication) {
        ResultBody<UserAccount> userAccountResultBody = baseUserServiceClient.userLogin(integrationAuthentication.getUsername());
        UserAccount userAccount = userAccountResultBody.getResult();
        return userAccount;
    }

    @Override
    public void prepare(IntegrationAuthentication integrationAuthentication) {

    }

    @Override
    public boolean support(IntegrationAuthentication integrationAuthentication) {
        return StrUtil.isEmpty(integrationAuthentication.getLoginType());
    }
}
