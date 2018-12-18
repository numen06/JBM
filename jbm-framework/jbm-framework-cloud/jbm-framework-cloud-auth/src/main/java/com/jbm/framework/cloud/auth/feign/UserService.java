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

package com.jbm.framework.cloud.auth.feign;

import com.jbm.framework.cloud.auth.feign.fallback.UserServiceFallbackImpl;
import com.jbm.framework.cloud.auth.model.JbmAuthUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author wesley
 * @date 2017/10/31
 */
@FeignClient(name = "hubao-platform-system", fallback = UserServiceFallbackImpl.class)
public interface UserService {

    /**
     * 通过用户名查询用户、角色信息
     *
     * @param username 用户名
     * @return UserVo
     */
    @PostMapping("/hubao-platform-system/authUser/findUserByUsername")
    JbmAuthUser findUserByUsername(@RequestParam("username") String username);

    /**
     * 通过手机号查询用户、角色信息
     *
     * @param mobile 手机号
     * @return UserVo
     */
    @PostMapping("/hubao-platform-system/authUser/findUserByMobile")
    JbmAuthUser findUserByMobile(@RequestParam("mobile") String mobile);

    /**
     * 根据OpenId查询用户信息
     *
     * @param openId openId
     * @return UserVo
     */
    @PostMapping("/hubao-platform-system/authUser/findUserByOpenId")
    JbmAuthUser findUserByOpenId(@RequestParam("openId") String openId);
}
