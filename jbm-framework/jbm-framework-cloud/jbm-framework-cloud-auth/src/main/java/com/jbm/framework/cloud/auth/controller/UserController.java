package com.jbm.framework.cloud.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class UserController {

    @RequestMapping(value = "/user")
    public Object getUser(Authentication authentication) {
        return authentication.getPrincipal();
    }
}
