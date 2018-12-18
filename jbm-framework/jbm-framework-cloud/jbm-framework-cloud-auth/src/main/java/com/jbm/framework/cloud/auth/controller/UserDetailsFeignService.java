package com.jbm.framework.cloud.auth.controller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

//@FeignClient(name = "${authserver.name:authserver}")
public interface UserDetailsFeignService {

    @RequestMapping("/loadUserByUsername")
    public UserDetails loadUserByUsername(@RequestParam("username") String username);
}
