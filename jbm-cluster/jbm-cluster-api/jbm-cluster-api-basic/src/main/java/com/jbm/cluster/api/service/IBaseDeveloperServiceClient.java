package com.jbm.cluster.api.service;

import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author wesley.zhang
 */
public interface IBaseDeveloperServiceClient {

    @PostMapping("/developer/sessions")
    ResultBody<UserAccount> developerLogin(@RequestParam("username") String username);

    @PostMapping("/developer/third-party-accounts")
    ResultBody<Void> addDeveloperThirdParty(
            @RequestParam("account") String account,
            @RequestParam("password") String password,
            @RequestParam("accountType") String accountType,
            @RequestParam("nickName") String nickName,
            @RequestParam("avatar") String avatar);
}
