package com.jbm.cluster.auth.service.authenticate;

import cn.hutool.core.lang.Validator;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.service.IBaseUserServiceClient;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.cluster.auth.service.PCoderService;
import com.jbm.cluster.auth.service.UserService;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
                return userService.findUserByUsername(username);
            }
        });
    }

    @Override
    public LoginType getLoginType() {
        return LoginType.SMS;
    }
}
