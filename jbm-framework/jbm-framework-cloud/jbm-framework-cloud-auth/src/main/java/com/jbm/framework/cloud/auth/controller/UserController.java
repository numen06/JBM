package com.jbm.framework.cloud.auth.controller;

import com.google.common.collect.Lists;
import com.jbm.framework.cloud.auth.model.JbmAuthUser;
import com.jbm.framework.metadata.bean.ResultBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.ConsumerTokenServices;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/")
@Slf4j
public class UserController {

    @RequestMapping(value = "/user")
    public Object getUser(Authentication authentication) {
        Object principla = authentication.getPrincipal();
        if (principla instanceof JbmAuthUser) {
            return principla;
        }
        JbmAuthUser user = new JbmAuthUser();
        user.setUsername(authentication.getPrincipal().toString());
        user.setPassword(UUID.randomUUID().toString());
        user.setEnabled(true);
        user.setUserId(0l);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setRoleList(Lists.newArrayList("ROLE_ADMIN"));
        return user;
    }

    @Autowired
    private ConsumerTokenServices tokenServices;

    @RequestMapping("/exit")
    public ResultBody revokeToken(OAuth2Authentication authentication) {
        try {
            if (authentication.getDetails() instanceof OAuth2AuthenticationDetails) {
                String token = ((OAuth2AuthenticationDetails) authentication.getDetails()).getTokenValue();
                tokenServices.revokeToken(token);
                log.info("token:{}退出登录", token);
                return ResultBody.success(token, "退出登录成功");
            }
        } catch (Exception e) {
            log.error("退出登录错误", e);
        }
        return ResultBody.error(null, "退出登录失败");
    }


}
