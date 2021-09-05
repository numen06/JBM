package com.jbm.cluster.api.service;

import com.jbm.cluster.api.model.UserAccount;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author wesley.zhang
 */
public interface IBaseUserServiceClient {

    /**
     * 系统用户登录
     *
     * @param username
     * @return
     */
    @PostMapping("/user/login")
    ResultBody<UserAccount> userLogin(@RequestParam(value = "username") String username);


    @PostMapping("/user/loginByType")
    ResultBody<UserAccount> userLoginByType(@RequestParam(value = "username") String username,
                                            @RequestParam(value = "loginType") String loginType);

    /**
     * 注册第三方系统登录账号
     *
     * @param account
     * @param password
     * @param accountType
     * @return
     */
    @PostMapping("/user/register/thirdParty")
    ResultBody addUserThirdParty(
            @RequestParam(value = "account") String account,
            @RequestParam(value = "password") String password,
            @RequestParam(value = "accountType") String accountType,
            @RequestParam(value = "nickName") String nickName,
            @RequestParam(value = "avatar") String avatar
    );


}
