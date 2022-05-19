package com.jbm.cluster.auth.controller;

import com.google.common.collect.Sets;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.auth.service.LoginAuthenticate;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Created wesley.zhang
 * @Date 2022/5/19 16:36
 * @Description TODO
 */
@RestController
public class LoginAuthenticateController implements LoginAuthenticate {

    @Override
    @PostMapping(value = {"/authenticate/{loginType}/login"})
    public ResultBody<JbmLoginUser> login(String username, String password, @PathVariable("loginType") String loginType) {
        if ("sa".equals(username) && "123456".equals(password)) {
            JbmLoginUser jbmLoginUser = new JbmLoginUser();
            jbmLoginUser.setRoles(Sets.newHashSet("user"));
            jbmLoginUser.setUsername("sa");
            jbmLoginUser.setUserId(1l);
            return ResultBody.ok().data(jbmLoginUser);
        }
        return ResultBody.error("账号名密码错误");
    }
}
