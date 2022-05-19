package com.jbm.cluster.auth.service;

import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 登录认证接口
 *
 * @Created wesley.zhang
 * @Date 2022/5/19 13:21
 * @Description TODO
 */
public interface LoginAuthenticate {

    @PostMapping(value = {"/authenticate/{loginType}/login"}, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    ResultBody<JbmLoginUser> login(@RequestParam("username") String username, @RequestParam("password") String password, @PathVariable("loginType") String loginType);

}
