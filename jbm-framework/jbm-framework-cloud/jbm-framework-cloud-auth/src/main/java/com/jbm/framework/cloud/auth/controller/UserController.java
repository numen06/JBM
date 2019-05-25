package com.jbm.framework.cloud.auth.controller;

import com.google.common.collect.Lists;
import com.jbm.framework.cloud.auth.model.JbmAuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/")
public class UserController {

    @RequestMapping(value = "/user")
    public Object getUser(Authentication authentication) {
        Object principla = authentication.getPrincipal();
        if (principla instanceof JbmAuthUser) {
            return principla;
        }
        JbmAuthUser user = new JbmAuthUser();
        user.setUsername(authentication.getPrincipal().toString());
        user.setPassword(new BCryptPasswordEncoder().encode(UUID.randomUUID().toString()));
        user.setEnabled(true);
        user.setUserId(0l);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setRoleList(Lists.newArrayList("ROLE_ADMIN"));
        return user;
    }
}
