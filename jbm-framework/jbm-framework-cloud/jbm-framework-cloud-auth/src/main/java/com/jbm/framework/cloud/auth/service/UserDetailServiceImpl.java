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

package com.jbm.framework.cloud.auth.service;

import com.jbm.framework.cloud.auth.feign.UserService;
import com.jbm.framework.cloud.auth.model.JbmAuthUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @author wesley
 * @date 2017/10/26
 * <p>
 */
@Service("userDetailService")
public class UserDetailServiceImpl implements UserDetailsService {
    @Autowired
    private UserService userService;
    @Autowired
    private DiscoveryClient discoveryClient;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        JbmAuthUser userDetails = userService.findUserByUsername(username);

        if (userDetails == null) {
            throw new UsernameNotFoundException("用户名不存在或者密码错误");
        }
//        return new UserDetailsImpl(userVo);
        return userDetails;
    }
}
