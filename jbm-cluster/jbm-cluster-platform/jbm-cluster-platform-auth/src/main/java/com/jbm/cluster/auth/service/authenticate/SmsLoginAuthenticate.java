package com.jbm.cluster.auth.service.authenticate;

import cn.hutool.core.lang.Validator;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.cluster.auth.service.PCoderService;
import com.jbm.cluster.auth.service.UserService;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

@Service
public class SmsLoginAuthenticate implements ILoginAuthenticate {

    @Autowired
    private PCoderService pCoderService;
    @Autowired
    private UserService userService;


    @Override
    public ResultBody<JbmLoginUser> login(String username, String password, String loginType) {
        return ResultBody.callback("短信登录成功", new Supplier<JbmLoginUser>() {
            @Override
            public JbmLoginUser get() {
                Validator.validateMobile(username, "非法手机号");
                pCoderService.verify(password, username);
                return userService.loginAndRegisterMobileUser(username, password);
            }
        });
    }

    @Override
    public List<LoginType> getLoginType() {
        return Lists.newArrayList(LoginType.SMS);
    }
}
