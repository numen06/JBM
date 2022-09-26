package com.jbm.cluster.api.service;

import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 登录认证接口
 *
 * @Created wesley.zhang
 * @Date 2022/5/19 13:21
 * @Description TODO
 */
public interface ILoginAuthenticate {

    @PostMapping(value = {"/authenticate/{loginType}/login"}, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    ResultBody<JbmLoginUser> login(@RequestParam("username") String username, @RequestParam("password") String password, @PathVariable("loginType") String loginType);

    @GetMapping(value = {"/authenticate/getLoginType"})
    List<LoginType> getLoginType();
}
