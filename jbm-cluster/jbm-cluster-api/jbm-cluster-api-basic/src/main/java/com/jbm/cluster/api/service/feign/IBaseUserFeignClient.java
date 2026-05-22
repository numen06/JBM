package com.jbm.cluster.api.service.feign;

import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.ThirdPartyUserForm;
import com.jbm.cluster.api.model.auth.UserAccount;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface IBaseUserFeignClient {

    @GetMapping("/{userId}")
    BaseUser getUser(@PathVariable("userId") Long userId);

    @PostMapping("/sessions")
    UserAccount userLogin(@RequestParam("username") String username, @RequestParam(value = "loginType", required = false) String loginType);

    @PostMapping("/registrations")
    Void register(@RequestParam(value = "registerIp", required = false) String registerIp, @RequestParam("userName") String userName, @RequestParam(value = "nickName", required = false) String nickName, @RequestParam(value = "accountType", required = false) String accountType, @RequestParam("password") String password, @RequestParam("confirmPassword") String confirmPassword);

    @PutMapping("/{userId}")
    Void updateUser(@PathVariable("userId") Long userId, @RequestBody BaseUser user);

    @PostMapping("/third-party-accounts")
    Void addUserThirdParty(@RequestParam("account") String account, @RequestParam("password") String password, @RequestParam("accountType") String accountType, @RequestParam("nickName") String nickName, @RequestParam("avatar") String avatar);

    @PostMapping("/sessions/mobile")
    UserAccount loginAndRegisterMobileUser(@RequestBody ThirdPartyUserForm thirdPartyUserForm);

    @PostMapping("/third-party-account-bindings")
    Void bindUserThirdPartyByPhone(@RequestParam("account") String account, @RequestParam("password") String password, @RequestParam("accountType") String accountType, @RequestParam("phone") String phone);

    @GetMapping(params = "phone")
    BaseUser getUserByPhone(@RequestParam("phone") String phone);

    @GetMapping("/{userId}/accounts")
    List<BaseAccount> getUserAccounts(@PathVariable("userId") Long userId);
}