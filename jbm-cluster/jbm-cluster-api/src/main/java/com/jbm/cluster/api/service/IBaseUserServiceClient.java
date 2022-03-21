package com.jbm.cluster.api.service;

import com.jbm.cluster.api.form.ThirdPartyUserForm;
import com.jbm.cluster.api.model.UserAccount;
import com.jbm.cluster.api.model.entity.BaseUser;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @ApiOperation(value = "更新系统用户", notes = "更新系统用户")
    @PostMapping(value = "/user/update", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    ResultBody updateUser(BaseUser user);

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


    @ApiOperation(value = "注册并登录第三方系统登录账号", notes = "仅限系统内部调用")
    @PostMapping("/user/loginAndRegisterMobileUser")
    ResultBody<UserAccount> loginAndRegisterMobileUser(@RequestBody ThirdPartyUserForm thirdPartyUserForm);

    @ApiOperation(value = "注册第三方系统登录账号", notes = "仅限系统内部调用")
    @PostMapping("/user/add/bindUserThirdPartyByPhone")
    ResultBody bindUserThirdPartyByPhone(
            @RequestParam(value = "account") String account,
            @RequestParam(value = "password") String password,
            @RequestParam(value = "accountType") String accountType,
            @RequestParam(value = "phone") String phone
    );

    @ApiOperation(value = "模糊搜索用户")
    @PostMapping("/user/getUserByPhone")
    ResultBody<BaseUser> getUserByPhone(@RequestParam(value = "phone") String phone);
}
