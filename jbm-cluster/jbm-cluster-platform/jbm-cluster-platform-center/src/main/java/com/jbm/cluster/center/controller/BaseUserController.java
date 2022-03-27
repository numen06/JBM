package com.jbm.cluster.center.controller;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.lang.Validator;
import cn.hutool.extra.servlet.ServletUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.cluster.api.form.ThirdPartyUserForm;
import com.jbm.cluster.api.model.UserAccount;
import com.jbm.cluster.api.model.entity.BaseAccount;
import com.jbm.cluster.api.model.entity.BaseRole;
import com.jbm.cluster.api.model.entity.BaseUser;
import com.jbm.cluster.api.service.IBaseUserServiceClient;
import com.jbm.cluster.center.service.BaseRoleService;
import com.jbm.cluster.center.service.BaseUserService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.PasswordUtils;
import com.jbm.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.xml.ws.Service;
import java.util.List;
import java.util.Map;

/**
 * 系统用户信息
 *
 * @author wesley.zhang
 */
@Api(tags = "系统用户管理")
@RestController
@RequestMapping("/user")
public class BaseUserController extends MasterDataCollection<BaseUser, BaseUserService> implements IBaseUserServiceClient {
    @Autowired
    private BaseUserService baseUserService;
    @Autowired
    private BaseRoleService baseRoleService;


    /**
     * 获取登录账号信息
     *
     * @param username 登录名
     * @return
     */
    @ApiOperation(value = "获取账号登录信息", notes = "仅限系统内部调用")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", required = true, value = "登录名", paramType = "path"),
    })
    @PostMapping("/login")
    @Override
    public ResultBody<UserAccount> userLogin(@RequestParam(value = "username") String username) {
        UserAccount account = baseUserService.login(username);
        return ResultBody.ok().data(account);
    }

    /**
     * 获取登录账号信息
     *
     * @param username 登录名
     * @return
     */
    @ApiOperation(value = "获取账号登录信息", notes = "仅限系统内部调用")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", required = true, value = "登录名", paramType = "path"),
            @ApiImplicitParam(name = "loginType", required = false, value = "登录类型：mobile,password", paramType = "path"),
    })
    @PostMapping("/loginByType")
    @Override
    public ResultBody<UserAccount> userLoginByType(@RequestParam(value = "username") String username,
                                                   @RequestParam(value = "loginType") String loginType) {
        UserAccount account = baseUserService.login(username, loginType);
        return ResultBody.ok().data(account);
    }


    /**
     * 系统分页用户列表
     *
     * @return
     */
    @ApiOperation(value = "PostMapping系统分页用户列表", notes = "系统分页用户列表")
    @PostMapping("")
    public ResultBody<IPage<BaseUser>> getUserList(@RequestParam(required = false) Map map) {
        return ResultBody.ok().data(baseUserService.findListPage(PageRequestBody.from(map)));
    }

    /**
     * 获取所有用户列表
     *
     * @return
     */
    @ApiOperation(value = "获取所有用户列表", notes = "获取所有用户列表")
    @PostMapping("/all")
    public ResultBody<List<BaseRole>> getUserAllList() {
        return ResultBody.ok().data(baseUserService.findAllList());
    }

    /**
     * 添加系统用户
     *
     * @return
     */
    @ApiOperation(value = "注册账号", notes = "添加系统用户")
    @PostMapping("/register")
    @Override
    public ResultBody register(@RequestParam(value = "registerIp", required = false) String registerIp,
                               @RequestParam(value = "userName") String userName,
                               @RequestParam(value = "nickName", required = false) String nickName,
                               @RequestParam(value = "accountType", required = false) String accountType,
                               @RequestParam(value = "password") String password,
                               @RequestParam(value = "confirmPassword") String confirmPassword
    ) {
        try {
//            Validator.validateEmpty(userName, "用户名为空");
            if (userName.length() < 2) {
                throw new ValidateException("用户名少于两个字符");
            }
            PasswordUtils.validatorPassword("", password, confirmPassword);
            BaseUser user = new BaseUser();
            user.setUserName(userName);
            user.setPassword(password);
            user.setNickName(nickName);
            baseUserService.register(user, registerIp);
            return ResultBody.ok().msg("注册账号成功");
        } catch (ValidateException e) {
            return ResultBody.failed().msg(e.getMessage());
        } catch (ServiceException e) {
            return ResultBody.failed().msg(e.getMessage());
        } catch (Exception e) {
            return ResultBody.failed().msg("帐号注册错误").exception(e);
        }
    }

    /**
     * 添加系统用户
     *
     * @param userName
     * @param password
     * @param nickName
     * @param status
     * @param userType
     * @param email
     * @param mobile
     * @param userDesc
     * @param avatar
     * @return
     */
    @ApiOperation(value = "添加系统用户", notes = "添加系统用户")
    @PostMapping("/add")
    public ResultBody<Long> addUser(
            @RequestParam(value = "userName") String userName,
            @RequestParam(value = "password") String password,
            @RequestParam(value = "nickName") String nickName,
            @RequestParam(value = "status") Integer status,
            @RequestParam(value = "userType") String userType,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "mobile", required = false) String mobile,
            @RequestParam(value = "userDesc", required = false) String userDesc,
            @RequestParam(value = "avatar", required = false) String avatar
    ) {
        BaseUser user = new BaseUser();
        user.setUserName(userName);
        user.setPassword(password);
        user.setNickName(nickName);
        user.setUserType(userType);
        user.setEmail(email);
        user.setMobile(mobile);
        user.setUserDesc(userDesc);
        user.setAvatar(avatar);
        user.setStatus(status);
        baseUserService.addUser(user);
        return ResultBody.ok();
    }

    /**
     * 更新系统用户
     *
     * @return
     */
    @ApiOperation(value = "更新系统用户", notes = "更新系统用户")
    @PostMapping("/update")
    @Override
    public ResultBody updateUser(BaseUser user) {
//        BaseUser user = new BaseUser();
//        user.setUserId(userId);
//        user.setNickName(nickName);
//        user.setUserType(userType);
//        user.setEmail(email);
//        user.setMobile(mobile);
//        user.setUserDesc(userDesc);
//        user.setAvatar(avatar);
//        user.setStatus(status);
        baseUserService.updateUser(user);
        return ResultBody.ok();
    }


    /**
     * 修改用户密码
     *
     * @param userId
     * @param password
     * @return
     */
    @ApiOperation(value = "修改用户密码", notes = "修改用户密码")
    @PostMapping("/update/password")
    public ResultBody updatePassword(
            @RequestParam(value = "userId") Long userId,
            @RequestParam(value = "password") String password
    ) {
        baseUserService.updatePassword(userId, password);
        return ResultBody.ok().msg("修改密码成功");
    }


    /**
     * 修改用户密码
     *
     * @return
     */
    @ApiOperation(value = "激活用户Email帐号", notes = "用户ID必传")
    @PostMapping("/activationEmailAccount")
    public ResultBody activationEmailAccount(BaseUser baseUser) {
        baseUserService.activationEmailAccount(baseUser);
        return ResultBody.ok().msg("激活用户Email帐号成功");
    }


    /**
     * 修改用户密码
     *
     * @return
     */
    @ApiOperation(value = "激活用户手机帐号", notes = "用户ID必传")
    @PostMapping("/activationMobileAccount")
    public ResultBody activationMobileAccount(BaseUser baseUser) {
        baseUserService.activationMobileAccount(baseUser);
        return ResultBody.ok().msg("激活用户手机帐号成功");
    }

    /**
     * 用户分配角色
     *
     * @param userId
     * @param roleIds
     * @return
     */
    @ApiOperation(value = "用户分配角色", notes = "用户分配角色")
    @PostMapping("/roles/add")
    public ResultBody addUserRoles(
            @RequestParam(value = "userId") Long userId,
            @RequestParam(value = "roleIds", required = false) String roleIds
    ) {
        baseRoleService.saveUserRoles(userId, StringUtils.isNotBlank(roleIds) ? roleIds.split(",") : new String[]{});
        return ResultBody.ok();
    }


    /**
     * 用户分配角色
     *
     * @return
     */
    @ApiOperation(value = "用户分配角色", notes = "用户分配角色")
    @PostMapping("/addRole")
    public ResultBody addUserRoles(@RequestBody BaseUserForm baseUserForm) {
        baseRoleService.saveUserRoles(baseUserForm.getUserId(), baseUserForm.getRoleIds());
        return ResultBody.ok();
    }

    /**
     * 获取用户角色
     *
     * @param userId
     * @return
     */
    @ApiOperation(value = "获取用户已分配角色", notes = "获取用户已分配角色")
    @PostMapping("/roles")
    public ResultBody<List<BaseRole>> getUserRoles(@RequestParam(value = "userId") Long userId) {
        return ResultBody.ok().data(baseRoleService.getUserRoles(userId));
    }


    @ApiOperation(value = "获取用户已分配角色", notes = "获取用户已分配角色")
    @PostMapping("/userRoles")
    public ResultBody<List<BaseRole>> getUserRoles(@RequestBody BaseUser baseUser) {
        return ResultBody.ok().data(baseRoleService.getUserRoles(baseUser.getUserId()));
    }


    /**
     * 注册第三方系统登录账号
     *
     * @param account
     * @param password
     * @param accountType
     * @return
     */
    @ApiOperation(value = "注册第三方系统登录账号", notes = "仅限系统内部调用")
    @PostMapping("/add/thirdParty")
    @Override
    public ResultBody addUserThirdParty(
            @RequestParam(value = "account") String account,
            @RequestParam(value = "password") String password,
            @RequestParam(value = "accountType") String accountType,
            @RequestParam(value = "nickName") String nickName,
            @RequestParam(value = "avatar") String avatar
    ) {
        BaseUser user = new BaseUser();
        user.setNickName(nickName);
        user.setUserName(account);
        user.setPassword(password);
        user.setAvatar(avatar);
        baseUserService.addUserThirdParty(user, accountType);
        return ResultBody.ok();
    }

    @ApiOperation(value = "注册并登录第三方系统登录账号", notes = "仅限系统内部调用")
    @PostMapping("/loginAndRegisterMobileUser")
    @Override
    public ResultBody<UserAccount> loginAndRegisterMobileUser(@RequestBody ThirdPartyUserForm thirdPartyUserForm) {
        try {
            UserAccount userAccount = baseUserService.loginAndRegisterMobileUser(thirdPartyUserForm);
            return ResultBody.ok().data(userAccount);
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "绑定第三方系统登录账号", notes = "仅限系统内部调用")
    @PostMapping("/add/bindUserThirdPartyByPhone")
    @Override
    public ResultBody bindUserThirdPartyByPhone(@RequestParam(value = "account") String account,
                                                @RequestParam(value = "password") String password,
                                                @RequestParam(value = "accountType") String accountType,
                                                @RequestParam(value = "phone") String phone) {
        BaseAccount baseAccount = new BaseAccount();
        baseAccount.setAccount(account);
        baseAccount.setPassword(password);
        baseAccount.setAccountType(accountType);
        baseUserService.bindUserThirdPartyByPhone(phone, baseAccount);
        return ResultBody.ok();
    }


    @ApiOperation(value = "模糊搜索用户")
    @PostMapping("/retrievalUsers")
    public ResultBody<List<BaseUser>> retrievalUsers(@RequestBody PageForm pageForm) {
        try {
            List<BaseUser> list = Lists.newArrayList();
            list = baseUserService.retrievalUsers(pageForm.getKeyword());
            return ResultBody.success(list, "模糊搜索用户成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "模糊搜索用户")
    @PostMapping("/getUserByPhone")
    @Override
    public ResultBody<BaseUser> getUserByPhone(String phone) {
        try {
            BaseUser user = baseUserService.getUserByPhone(phone);
            return ResultBody.success(user, "查找用户成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }


}
