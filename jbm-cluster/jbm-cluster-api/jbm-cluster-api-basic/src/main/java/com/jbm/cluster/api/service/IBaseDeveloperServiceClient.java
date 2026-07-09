package com.jbm.cluster.api.service;

import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.core.annotation.InnerAuth;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author wesley.zhang
 */
public interface IBaseDeveloperServiceClient {

    /**
     * 开发者登录
     *
     * @param username
     * @return
     */
    @InnerAuth
    @PostMapping("/developer/login")
    ResultBody<UserAccount> developerLogin(@RequestParam(value = "username") String username);


    @InnerAuth
    @PostMapping("/developer/register/thirdParty")
    ResultBody addDeveloperThirdParty(
            @RequestParam(value = "account") String account,
            @RequestParam(value = "password") String password,
            @RequestParam(value = "accountType") String accountType,
            @RequestParam(value = "nickName") String nickName,
            @RequestParam(value = "avatar") String avatar
    );


}
