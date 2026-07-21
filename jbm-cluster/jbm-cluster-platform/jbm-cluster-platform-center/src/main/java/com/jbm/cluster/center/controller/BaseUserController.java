package com.jbm.cluster.center.controller;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.cluster.api.form.ThirdPartyUserForm;
import com.jbm.cluster.api.form.user.UserInfoStatistics;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.api.service.IBaseUserServiceClient;
import com.jbm.cluster.center.controller.authenticate.LoginAuthenticateHelper;
import com.jbm.cluster.center.service.BaseAccountService;
import com.jbm.cluster.center.service.BaseRoleService;
import com.jbm.cluster.center.service.BaseUserService;
import com.jbm.cluster.common.basic.log.annotation.OperatorLog;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.form.IdsForm;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.PasswordUtils;
import com.jbm.util.StringUtils;
import com.jbm.util.sensitive.SensitiveContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
    @Autowired
    private LoginAuthenticateHelper loginAuthenticateHelper;

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "通过用户名查询用户信息", notes = "仅限系统内部 Feign 调用")
    @GetMapping("/info/{username}")
    public ResultBody<JbmLoginUser> getUserInfo(@PathVariable("username") String username,
                                                @RequestHeader(JbmSecurityConstants.FROM_SOURCE) String source) {
        return ResultBody.callback(() -> {
            UserAccount account = baseUserService.login(username, null);
            if (account == null) {
                throw new ServiceException("用户不存在");
            }
            return loginAuthenticateHelper.userAccountToLoginUser(account);
        });
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "注册用户信息", notes = "仅限系统内部 Feign 调用")
    @PostMapping(value = "/register", consumes = "application/json")
    public ResultBody<Boolean> registerUserInfo(@RequestBody BaseUser baseUser,
                                                @RequestHeader(JbmSecurityConstants.FROM_SOURCE) String source) {
        baseUserService.register(baseUser, null);
        return ResultBody.ok(true);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @Override
    public ResultBody<List<BaseUser>> list(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        try {
            validator(masterDataRequsetBody);
            BaseUserForm baseUserForm = masterDataRequsetBody.tryGet(BaseUserForm.class);
            if (ObjectUtil.isNotEmpty(baseUserForm.getDateRange())) {
                baseUserForm.setBeginTime(baseUserForm.getDateRange()[0]);
                baseUserForm.setEndTime(baseUserForm.getDateRange()[1]);
            }
            List<BaseUser> list = this.service.selectEntitys(baseUserForm);
            return ResultBody.success(list, "查询列表成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @Override
    public ResultBody<DataPaging<BaseUser>> pageList(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        try {
            validator(pageRequestBody);
            BaseUserForm baseUserForm = pageRequestBody.tryGet(BaseUserForm.class);
            if (ObjectUtil.isNotEmpty(baseUserForm.getDateRange())) {
                baseUserForm.setBeginTime(baseUserForm.getDateRange()[0]);
                baseUserForm.setEndTime(baseUserForm.getDateRange()[1]);
            }
            PageForm pageForm = pageRequestBody.getPageForm();
            DataPaging<BaseUser> dataPaging = this.service.selectEntitys(baseUserForm, pageForm);
            return ResultBody.success(dataPaging, "查询分页列表成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "通过id获取用户信息", notes = "仅限系统内部调用，返回完整姓名等字段（不脱敏）")
    @GetMapping("/getUserInfoById")
    @Override
    public ResultBody<BaseUser> getUserInfoById(@RequestParam(value = "userId") Long userId) {
        SensitiveContext.skipMask();
        return ResultBody.callback(() -> this.service.selectById(userId));
    }

    /**
     * 获取登录账号信息
     *
     * @param username 登录名
     * @return
     */
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取账号登录信息", notes = "仅限系统内部调用")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", required = true, value = "登录名", paramType = "path"),
    })
    @PostMapping("/login")
    @Override
    public ResultBody<UserAccount> userLogin(@RequestParam(value = "username") String username) {
        UserAccount account = baseUserService.login(username);
        return ResultBody.callback(() -> account);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "保存用户信息")
    @PostMapping("/save")
    @OperatorLog
    @Override
    public ResultBody<BaseUser> save(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return ResultBody.callback("保存用户信息成功", () -> {
            validator(masterDataRequsetBody);
            BaseUser entity = validatorMasterData(masterDataRequsetBody, true);
            entity = service.saveEntity(entity);
            BaseUserForm baseUserForm = masterDataRequsetBody.toJavaObject(BaseUserForm.class);
            if (ObjectUtil.isNotEmpty(baseUserForm.getRoleIds())) {
                baseRoleService.saveUserRoles(entity.getUserId(), baseUserForm.getRoleIds());
            }
            return entity;
        });
    }

    /**
     * 获取登录账号信息
     *
     * @param username 登录名
     * @return
     */
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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
        return ResultBody.callback(() -> account);
    }


    /**
     * 系统分页用户列表
     *
     * @return
     */
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "PostMapping系统分页用户列表", notes = "系统分页用户列表")
    @PostMapping("")
    public ResultBody<DataPaging<BaseUser>> getUserList(@RequestParam(required = false) Map map) {
        return ResultBody.callback(() -> baseUserService.findListPage(PageRequestBody.from(map)));
    }

    /**
     * 获取所有用户列表
     *
     * @return
     */
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取所有用户列表", notes = "获取所有用户列表")
    @PostMapping("/all")
    public ResultBody<List<BaseUser>> getUserAllList() {
        return ResultBody.callback(() -> baseUserService.findAllList());
    }

    /**
     * 添加系统用户
     *
     * @return
     */
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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
            if (!Pattern.matches(JbmConstants.ACCOUNT_REGEX, userName)) {
                throw new ValidateException("用户名长度在 5 到 16 个字符");
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

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "注销账号", notes = "申请注销账号")
    @PostMapping("/close")
    public ResultBody<Boolean> close(@RequestBody BaseUser baseUser) {
        return ResultBody.callback(() -> this.service.close(baseUser));
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
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "添加系统用户", notes = "添加系统用户")
    @OperatorLog
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
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "激活用户Email帐号", notes = "用户ID必传")
    @PostMapping("/activationEmailAccount")
    public ResultBody activationEmailAccount(@RequestBody BaseUser baseUser) {
        baseUserService.activationEmailAccount(baseUser);
        return ResultBody.ok().msg("激活用户Email帐号成功");
    }


    /**
     * 修改用户密码
     *
     * @return
     */
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "激活用户手机帐号", notes = "用户ID必传")
    @PostMapping("/activationMobileAccount")
    public ResultBody activationMobileAccount(@RequestBody BaseUser baseUser) {
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
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取用户已分配角色", notes = "获取用户已分配角色")
    @PostMapping("/roles")
    public ResultBody<List<BaseRole>> getUserRoles(@RequestParam(value = "userId") Long userId) {
        return ResultBody.callback(() -> baseUserService.getUserRoles(userId));
    }


    @ApiOperation(value = "获取用户已分配角色", notes = "获取用户已分配角色")
    @PostMapping("/userRoles")
    public ResultBody<List<BaseRole>> getUserRoles(@RequestBody BaseUser baseUser) {
        return ResultBody.callback(() -> baseUserService.getUserRoles(baseUser.getUserId()));
    }


    /**
     * 注册第三方系统登录账号
     *
     * @param account
     * @param password
     * @param accountType
     * @return
     */
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "注册并登录第三方系统登录账号", notes = "仅限系统内部调用")
    @PostMapping("/loginAndRegisterMobileUser")
    @Override
    public ResultBody<UserAccount> loginAndRegisterMobileUser(@RequestBody ThirdPartyUserForm thirdPartyUserForm) {
        try {
            UserAccount userAccount = baseUserService.loginAndRegisterMobileUser(thirdPartyUserForm);
            return ResultBody.callback(() -> userAccount);
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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


    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "用户统计")
    @GetMapping("/getUserInfoStatistics")
    public ResultBody<UserInfoStatistics> getUserInfoStatistics() {
        return ResultBody.callback(() -> {
            UserInfoStatistics userInfoStatistics = new UserInfoStatistics();
            List<String> list = StpUtil.searchTokenValue("", -1, 0, true);
            userInfoStatistics.setOnlineUser(new Long(list.size()));
            userInfoStatistics.setUsersTotal(baseUserService.count(new BaseUser()));
            return userInfoStatistics;
        });
    }


    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "通过Ids获取多个用户")
    @GetMapping("/getUsersByIds")
    public ResultBody<List<BaseUser>> getUsersByIds(@RequestBody IdsForm ids) {
        return ResultBody.callback(() -> {
            return baseUserService.getUsersByIds(ids.getIds());
        });
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "改变用户状态")
    @GetMapping("/updateUserStatus")
    public ResultBody<BaseUser> updateUserStatus(@RequestParam(value = "userId") Long userId,
                                                 @RequestParam(value = "status") Integer status) {
        return ResultBody.callback(() -> {
            BaseUser baseUser = new BaseUser();
            baseUser.setUserId(userId);
            baseUser.setStatus(status);
            baseUserService.updateUser(baseUser);
            return baseUser;
        });
    }


    @Autowired
    private BaseAccountService baseAccountService;

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取用户帐号信息")
    @GetMapping("/getUserAccounts")
    @Override
    public ResultBody<List<BaseAccount>> getUserAccounts(@RequestParam(value = "userId") Long userId) {
        return ResultBody.callback("查询用户帐号列表成功", () -> {
            return baseAccountService.getUserAccounts(userId);
        });
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "小程序登录-更新用户openId信息")
    @PostMapping("/updateOpenIdByPhone")
    public ResultBody<BaseUser> updateOpenIdByPhone(@RequestParam(value = "openId") String openId,
                                                    @RequestParam(value = "sessionKey") String sessionKey,
                                                    @RequestParam(value = "accountType") String accountType,
                                                    @RequestParam(value = "phone") String phone) {
        return ResultBody.callback(() -> baseAccountService.updateOpenIdByPhone(openId, sessionKey, accountType, phone));
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取单个实体", notes = "获取单个实体")
    @PostMapping("/model")
    @Override
    public ResultBody<BaseUser> model(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.model(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "批量保存", notes = "批量保存")
    @PostMapping("/saveBatch")
    @Override
    public ResultBody<List<BaseUser>> saveBatch(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.saveBatch(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "模拟数据", notes = "模拟数据")
    @PostMapping("/mock")
    @Override
    public ResultBody<BaseUser> mock() {
        return super.mock();
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "删除实体", notes = "删除实体")
    @PostMapping("/delete")
    @Override
    public ResultBody<Boolean> remove(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.remove(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "通过id删除实体", notes = "通过id删除实体")
    @PostMapping("/deleteByIds")
    @Override
    public ResultBody<Boolean> deleteByIds(@RequestBody(required = false) IdsForm idsForm) {
        return super.deleteByIds(idsForm);
    }

}
