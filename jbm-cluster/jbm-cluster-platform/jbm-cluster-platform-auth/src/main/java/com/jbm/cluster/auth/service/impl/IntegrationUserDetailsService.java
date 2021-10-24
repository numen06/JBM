package com.jbm.cluster.auth.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.model.UserAccount;
import com.jbm.cluster.auth.integration.IntegrationAuthentication;
import com.jbm.cluster.auth.integration.IntegrationAuthenticationContext;
import com.jbm.cluster.auth.integration.IntegrationAuthenticator;
import com.jbm.cluster.auth.service.AccountUtils;
import com.jbm.cluster.common.security.OpenUserDetails;
import com.jbm.cluster.common.security.oauth2.client.JbmOAuth2ClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 集成认证用户服务
 *
 * @author wesley.zhang
 * @date 2018-3-7
 **/
@Service
@Slf4j
public class IntegrationUserDetailsService implements UserDetailsService {

    @Autowired
    private JbmOAuth2ClientProperties clientProperties;

    private List<IntegrationAuthenticator> authenticators;

    @Autowired(required = false)
    public void setIntegrationAuthenticators(List<IntegrationAuthenticator> authenticators) {
        this.authenticators = authenticators;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        IntegrationAuthentication integrationAuthentication = IntegrationAuthenticationContext.get();
        //判断是否是集成登录
        if (integrationAuthentication == null) {
            integrationAuthentication = new IntegrationAuthentication();
        }
        integrationAuthentication.setUsername(username);
        UserAccount sysUserAuthentication = this.authenticate(integrationAuthentication);
        if (sysUserAuthentication == null) {
            log.warn("用户名密码错误");
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        OpenUserDetails userDetails = AccountUtils.setAuthorize(sysUserAuthentication, username);
        if (ObjectUtil.isEmpty(userDetails.getClientId()))
            userDetails.setClientId(clientProperties.getOauth2().get("admin").getClientId());
        return userDetails;
    }


    private UserAccount authenticate(IntegrationAuthentication integrationAuthentication) {
        if (this.authenticators != null) {
            for (IntegrationAuthenticator authenticator : authenticators) {
                if (authenticator.support(integrationAuthentication)) {
                    return authenticator.authenticate(integrationAuthentication);
                }
            }
        }
        return null;
    }
}
