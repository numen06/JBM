package com.jbm.cluster.center.controller.authenticate;

import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.center.service.BaseUserService;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @Created wesley.zhang
 * @Date 2022/5/19 16:36
 * @Description TODO
 */
//@RestController
public class LoginAuthenticateController {

    @Autowired
    private BaseUserService baseUserService;

    @PostMapping(value = {"/authenticate/{loginType}/login"})
    public ResultBody<JbmLoginUser> login(String username, String password, @PathVariable("loginType") String loginType) {
        return null;
    }

}
