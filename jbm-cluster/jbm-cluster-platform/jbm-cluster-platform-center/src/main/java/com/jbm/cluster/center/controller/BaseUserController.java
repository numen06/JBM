package com.jbm.cluster.center.controller;

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
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.api.service.IBaseUserServiceClient;
import com.jbm.cluster.common.mysql.service.BaseAccountService;
import com.jbm.cluster.common.mysql.service.BaseRoleService;
import com.jbm.cluster.center.business.BaseUserBusiness;
import com.jbm.cluster.common.basic.log.annotation.OperatorLog;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.form.IdsForm;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.BaseController;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.PasswordUtils;
import com.jbm.util.StringUtils;
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
public class BaseUserController extends BaseController implements IBaseUserServiceClient {
    @Autowired
    private BaseUserBusiness baseUserBusiness;
    @Autowired
    private BaseRoleService baseRoleService;

    public ResultBody<List<BaseUser>> list(@RequestBody(required = false) BaseUserForm form) {
        try {
            if (form == null) {
                form = new BaseUserForm();
            }
            if (ObjectUtil.isNotEmpty(form.getDateRange())) {
                form.setBeginTime(form.getDateRange()[0]);
                form.setEndTime(form.getDateRange()[1]);
            }
            List<BaseUser> list = baseUserBusiness.selectEntitys(form);
            return ResultBody.success(list, "查询列表成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    public ResultBody<DataPaging<BaseUser>> pageList(@RequestBody(required = false) BaseUserForm form) {
        try {
            if (form == null) {
                form = new BaseUserForm();
            }
            if (ObjectUtil.isNotEmpty(form.getDateRange())) {
                form.setBeginTime(form.getDateRange()[0]);
                form.setEndTime(form.getDateRange()[1]);
            }
            PageForm pageForm = form.getPageForm() != null ? form.getPageForm() : new PageForm();
            DataPaging<BaseUser> dataPaging = baseUserBusiness.selectEntitys(form, pageForm);
            return ResultBody.success(dataPaging, "查询分页列表成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "通过id获取用户信息", notes = "仅限系统内部调用")
    @GetMapping("/getUserInfoById")
    @Override
    public ResultBody<BaseUser> getUserInfoById(@RequestParam(value = "userId") Long userId) {
        return ResultBody.callback(() -> baseUserBusiness.selectById(userId));
    }

    /**
     * 获取登录账号信息
     *
     * @param username 登录名
     * @return
     */
    @ApiOperation(value = "获取账号登录信息", notes = "仅限系统内部调用")
    @ApiImplicitParams({
            @ApiImplicitParam(dataTypeClass = String.class, name = "username", required = true, value = "登录名", paramType = "path"),
    })
    @PostMapping("/login")
    @Override
    public ResultBody<UserAccount> userLogin(@RequestParam(value = "username") String username) {
        UserAccount account = baseUserBusiness.login(username);
        return ResultBody.callback(() -> account);
    }

    @ApiOperation(value = "保存用户信息")
    @PostMapping("/save")
    @OperatorLog
    public ResultBody<BaseUser> save(@RequestBody(required = false) BaseUserForm form) {
        return ResultBody.callback("保存用户信息成功", () -> {
            if (form == null) {
                throw new ServiceException("参数错误");
            }
            BaseUser entity = baseUserBusiness.saveEntity(form);
            if (ObjectUtil.isNotEmpty(form.getRoleIds())) {
                baseRoleService.saveUserRoles(entity.getUserId(), form.getRoleIds());
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
    @ApiOperation(value = "获取账号登录信息", notes = "仅限系统内部调用")
    @ApiImplicitParams({
            @ApiImplicitParam(dataTypeClass = String.class, name = "username", required = true, value = "登录名", paramType = "path"),
            @ApiImplicitParam(dataTypeClass = String.class, name = "loginType", required = false, value = "登录类型：mobile,password", paramType = "path"),
    })
    @PostMapping("/loginByType")
    @Override
    public ResultBody<UserAccount> userLoginByType(@RequestParam(value = "username") String username,
                                                   @RequestParam(value = "loginType") String loginType) {
        UserAccount account = baseUserBusiness.login(username, loginType);
        return ResultBody.callback(() -> account);
    }


    /**
     * 系统分页用户列表
     *
     * @return
     */
    @ApiOperation(value = "PostMapping系统分页用户列表", notes = "系统分页用户列表")
    @PostMapping("")
    public ResultBody<DataPaging<BaseUser>> getUserList(@ModelAttribute BaseUserForm form) {
        return ResultBody.callback(() -> baseUserBusiness.findListPage(form != null ? form : new BaseUserForm()));
    }

    /**
     * 获取所有用户列表
     *
     * @return
     */
    @ApiOperation(value = "获取所有用户列表", notes = "获取所有用户列表")
    @PostMapping("/all")
    public ResultBody<List<BaseUser>> getUserAllList() {
        return ResultBody.callback(() -> baseUserBusiness.findAllList());
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
            if (!Pattern.matches(JbmConstants.ACCOUNT_REGEX, userName)) {
                throw new ValidateException("用户名长度在 5 到 16 个字符");
            }
            PasswordUtils.validatorPassword("", password, confirmPassword);
            BaseUser user = new BaseUser();
            user.setUserName(userName);
            user.setPassword(password);
            user.setNickName(nickName);
            baseUserBusiness.register(user, registerIp);
            return ResultBody.ok().msg("注册账号成功");
        } catch (ValidateException e) {
            return ResultBody.failed().msg(e.getMessage());
        } catch (ServiceException e) {
            return ResultBody.failed().msg(e.getMessage());
        } catch (Exception e) {
            return ResultBody.failed().msg("帐号注册错误").exception(e);
        }
    }

    @ApiOperation(value = "注销账号", notes = "申请注销账号")
    @PostMapping("/close")
    public ResultBody<Boolean> close(@RequestBody BaseUser baseUser) {
        return ResultBody.callback(() -> baseUserBusiness.close(baseUser));
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
        baseUserBusiness.addUser(user);
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
        baseUserBusiness.updateUser(user);
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
        baseUserBusiness.updatePassword(userId, password);
        return ResultBody.ok().msg("修改密码成功");
    }


    /**
     * 修改用户密码
     *
     * @return
     */
    @ApiOperation(value = "激活用户Email帐号", notes = "用户ID必传")
    @PostMapping("/activationEmailAccount")
    public ResultBody activationEmailAccount(@RequestBody BaseUser baseUser) {
        baseUserBusiness.activationEmailAccount(baseUser);
        return ResultBody.ok().msg("激活用户Email帐号成功");
    }


    /**
     * 修改用户密码
     *
     * @return
     */
    @ApiOperation(value = "激活用户手机帐号", notes = "用户ID必传")
    @PostMapping("/activationMobileAccount")
    public ResultBody activationMobileAccount(@RequestBody BaseUser baseUser) {
        baseUserBusiness.activationMobileAccount(baseUser);
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
        return ResultBody.callback(() -> baseUserBusiness.getUserRoles(userId));
    }


    @ApiOperation(value = "获取用户已分配角色", notes = "获取用户已分配角色")
    @PostMapping("/userRoles")
    public ResultBody<List<BaseRole>> getUserRoles(@RequestBody BaseUser baseUser) {
        return ResultBody.callback(() -> baseUserBusiness.getUserRoles(baseUser.getUserId()));
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
        baseUserBusiness.addUserThirdParty(user, accountType);
        return ResultBody.ok();
    }

    @ApiOperation(value = "注册并登录第三方系统登录账号", notes = "仅限系统内部调用")
    @PostMapping("/loginAndRegisterMobileUser")
    @Override
    public ResultBody<UserAccount> loginAndRegisterMobileUser(@RequestBody ThirdPartyUserForm thirdPartyUserForm) {
        try {
            UserAccount userAccount = baseUserBusiness.loginAndRegisterMobileUser(thirdPartyUserForm);
            return ResultBody.callback(() -> userAccount);
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
        baseUserBusiness.bindUserThirdPartyByPhone(phone, baseAccount);
        return ResultBody.ok();
    }


    @ApiOperation(value = "模糊搜索用户")
    @PostMapping("/retrievalUsers")
    public ResultBody<List<BaseUser>> retrievalUsers(@RequestBody PageForm pageForm) {
        try {
            List<BaseUser> list = Lists.newArrayList();
            list = baseUserBusiness.retrievalUsers(pageForm.getKeyword());
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
            BaseUser user = baseUserBusiness.getUserByPhone(phone);
            return ResultBody.success(user, "查找用户成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @ApiOperation(value = "用户统计")
    @GetMapping("/getUserInfoStatistics")
    public ResultBody<UserInfoStatistics> getUserInfoStatistics() {
        return ResultBody.callback(() -> {
            UserInfoStatistics userInfoStatistics = new UserInfoStatistics();
            List<String> list = StpUtil.searchTokenValue("", -1, 0, true);
            userInfoStatistics.setOnlineUser(new Long(list.size()));
            userInfoStatistics.setUsersTotal(baseUserBusiness.count(new BaseUser()));
            return userInfoStatistics;
        });
    }


    @ApiOperation(value = "通过Ids获取多个用户")
    @GetMapping("/getUsersByIds")
    public ResultBody<List<BaseUser>> getUsersByIds( @RequestBody IdsForm ids) {
        return ResultBody.callback(() -> {
            return baseUserBusiness.getUsersByIds(ids.getIds());
        });
    }

    @ApiOperation(value = "改变用户状态")
    @GetMapping("/updateUserStatus")
    public ResultBody<BaseUser> updateUserStatus(@RequestParam(value = "userId") Long userId,
                                                 @RequestParam(value = "status") Integer status) {
        return ResultBody.callback(() -> {
            BaseUser baseUser = new BaseUser();
            baseUser.setUserId(userId);
            baseUser.setStatus(status);
            baseUserBusiness.updateUser(baseUser);
            return baseUser;
        });
    }


    @Autowired
    private BaseAccountService baseAccountService;

    @ApiOperation(value = "获取用户帐号信息")
    @GetMapping("/getUserAccounts")
    @Override
    public ResultBody<List<BaseAccount>> getUserAccounts(@RequestParam(value = "userId") Long userId) {
        return ResultBody.callback("查询用户帐号列表成功", () -> {
            return baseAccountService.getUserAccounts(userId);
        });
    }

    @ApiOperation(value = "小程序登录-更新用户openId信息")
    @PostMapping("/updateOpenIdByPhone")
    public ResultBody<BaseUser> updateOpenIdByPhone(@RequestParam(value = "openId") String openId,
                                                    @RequestParam(value = "sessionKey") String sessionKey,
                                                    @RequestParam(value = "accountType") String accountType,
                                                    @RequestParam(value = "phone") String phone) {
        return ResultBody.callback(() -> baseAccountService.updateOpenIdByPhone(openId, sessionKey, accountType, phone));
    }
}
