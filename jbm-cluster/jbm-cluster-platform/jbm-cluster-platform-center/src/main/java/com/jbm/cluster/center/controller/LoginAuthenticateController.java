package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.cluster.center.service.BaseUserService;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Created wesley.zhang
 * @Date 2022/5/19 16:36
 * @Description TODO
 */
@RestController
public class LoginAuthenticateController implements ILoginAuthenticate {

    @Autowired
    private BaseUserService baseUserService;

    @Override
    @PostMapping(value = {"/authenticate/{loginType}/login"})
    public ResultBody<JbmLoginUser> login(String username, String password, @PathVariable("loginType") String loginType) {
        UserAccount account = baseUserService.login(username);
        if (account == null) {
            return ResultBody.error("没有找到此用户");
        }
        JbmLoginUser jbmLoginUser = null;
        if (SecurityUtils.getPasswordEncoder().matches(password, account.getPassword())) {
            jbmLoginUser = new JbmLoginUser();
            jbmLoginUser.setUserId(account.getUserId());
            return ResultBody.ok().data(jbmLoginUser);
        } else {
            return ResultBody.error("密码错误");
        }
    }

    @Override
    public LoginType getLoginType() {
        return LoginType.PASSWORD;
    }
}
