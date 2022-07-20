package com.jbm.cluster.center.controller.authenticate;

import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.cluster.center.service.BaseUserService;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

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
