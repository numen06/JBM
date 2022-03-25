package com.jbm.cluster.auth.integration.authenticator;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.model.UserAccount;
import com.jbm.cluster.auth.integration.AbstractPreparableIntegrationAuthenticator;
import com.jbm.cluster.auth.integration.IntegrationAuthentication;
import com.jbm.cluster.auth.service.feign.BaseUserServiceClient;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
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
    private StringRedisTemplate stringRedisTemplate;
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
        String vcode = integrationAuthentication.getAuthParameter("vcode");
        if (StrUtil.isBlank(vcode)) {
            return;
        }
        //验证验证码
        String checkCode = stringRedisTemplate.opsForValue().get(this.getVcodePath(null));
        if (StrUtil.equalsIgnoreCase(vcode, checkCode)) {
        } else {
            throw new OAuth2Exception("验证码错误");
        }
    }

    public String getVcodePath(String scope) {
        String key = "/vcode/" + StrUtil.emptyToDefault(scope, "");
        return key;
    }

    @Override
    public boolean support(IntegrationAuthentication integrationAuthentication) {
        return StrUtil.isEmpty(integrationAuthentication.getLoginType());
    }
}
