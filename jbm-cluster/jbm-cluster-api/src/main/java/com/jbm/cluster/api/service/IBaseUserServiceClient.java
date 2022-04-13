package com.jbm.cluster.api.service;

import com.jbm.cluster.api.form.ThirdPartyUserForm;
import com.jbm.cluster.api.model.UserAccount;
import com.jbm.cluster.api.model.entity.BaseUser;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * @author wesley.zhang
 */
@RequestMapping("/user")
public interface IBaseUserServiceClient {


    /**
     * 查询实体
     *
     * @return
     */
    @GetMapping("/getUserInfoById")
    ResultBody<BaseUser> getUserInfo(@RequestParam(value = "userId") Long userId);

    /**
     * 系统用户登录
     *
     * @param username
     * @return
     */
    @PostMapping("/login")
    ResultBody<UserAccount> userLogin(@RequestParam(value = "username") String username);


    @PostMapping("/loginByType")
    ResultBody<UserAccount> userLoginByType(@RequestParam(value = "username") String username,
                                            @RequestParam(value = "loginType") String loginType);


    @ApiOperation(value = "注册账号", notes = "添加系统用户")
    @PostMapping("/register")
    ResultBody register(@RequestParam(value = "registerIp", required = false) String registerIp,
                        @RequestParam(value = "userName") String userName,
                        @RequestParam(value = "nickName", required = false) String nickName,
                        @RequestParam(value = "accountType", required = false) String accountType,
                        @RequestParam(value = "password") String password,
                        @RequestParam(value = "confirmPassword") String confirmPassword
    );

    @ApiOperation(value = "更新系统用户", notes = "更新系统用户")
    @PostMapping(value = "/update", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    ResultBody updateUser(BaseUser user);

    /**
     * 注册第三方系统登录账号
     *
     * @param account
     * @param password
     * @param accountType
     * @return
     */
    @PostMapping("/register/thirdParty")
    ResultBody addUserThirdParty(
            @RequestParam(value = "account") String account,
            @RequestParam(value = "password") String password,
            @RequestParam(value = "accountType") String accountType,
            @RequestParam(value = "nickName") String nickName,
            @RequestParam(value = "avatar") String avatar
    );


    @ApiOperation(value = "注册并登录第三方系统登录账号", notes = "仅限系统内部调用")
    @PostMapping("/loginAndRegisterMobileUser")
    ResultBody<UserAccount> loginAndRegisterMobileUser(@RequestBody ThirdPartyUserForm thirdPartyUserForm);

    @ApiOperation(value = "注册第三方系统登录账号", notes = "仅限系统内部调用")
    @PostMapping("/add/bindUserThirdPartyByPhone")
    ResultBody bindUserThirdPartyByPhone(
            @RequestParam(value = "account") String account,
            @RequestParam(value = "password") String password,
            @RequestParam(value = "accountType") String accountType,
            @RequestParam(value = "phone") String phone
    );

    @ApiOperation(value = "模糊搜索用户")
    @PostMapping("/getUserByPhone")
    ResultBody<BaseUser> getUserByPhone(@RequestParam(value = "phone") String phone);
}
