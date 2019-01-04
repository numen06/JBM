/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.jbm.framework.cloud.auth.component.mobile;

import com.jbm.framework.cloud.auth.feign.UserService;
import com.jbm.framework.cloud.auth.model.JbmAuthUser;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @author lengleng
 * @date 2018/1/9
 * 手机号登录校验逻辑
 */
public class MobileAuthenticationProvider implements AuthenticationProvider {
    private UserService userService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        MobileAuthenticationToken mobileAuthenticationToken = (MobileAuthenticationToken) authentication;
//        JbmUserEntity userVo = userService.findUserByMobile((String) mobileAuthenticationToken.getPrincipal());

    //    JbmAuthUser userDetails = userService.findUserByMobile((String) mobileAuthenticationToken.getPrincipal());
        JbmAuthUser userDetails = userService.findUserByMobileAndCode((String) mobileAuthenticationToken.getPrincipal(),mobileAuthenticationToken.getCode());
        if (userDetails == null) {
            throw new UsernameNotFoundException("手机号或验证码错误!手机号:" + mobileAuthenticationToken.getPrincipal()+"验证码:"+mobileAuthenticationToken.getCode());
        }
        //验证验证码是否正确
//        UserDetailsImpl userDetails = buildUserDeatils(userVo);

        MobileAuthenticationToken authenticationToken = new MobileAuthenticationToken(userDetails, userDetails.getAuthorities());
       // authenticationToken.setDetails(mobileAuthenticationToken.getDetails()); zpf
        return authenticationToken;
    }

//    private UserDetailsImpl buildUserDeatils(JbmUserEntity userVo) {
//        return new UserDetailsImpl(userVo);
//    }

    @Override
    public boolean supports(Class<?> authentication) {
        return MobileAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public UserService getUserService() {
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
