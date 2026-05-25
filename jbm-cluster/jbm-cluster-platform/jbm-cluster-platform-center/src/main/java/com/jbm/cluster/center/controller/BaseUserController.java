package com.jbm.cluster.center.controller;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.entitys.basic.BaseUserOrg;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.cluster.api.form.ThirdPartyUserForm;
import com.jbm.cluster.api.form.user.UserInfoStatistics;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.center.business.BaseUserBusiness;
import com.jbm.cluster.common.basic.log.annotation.OperatorLog;
import com.jbm.cluster.common.mysql.service.BaseAccountService;
import com.jbm.cluster.common.mysql.service.BaseRoleService;
import com.jbm.cluster.common.mysql.service.BaseUserOrgService;
import com.jbm.cluster.common.mysql.service.BaseUserService;
import com.jbm.cluster.common.mysql.service.OnlineUserFilter;
import com.jbm.cluster.common.mysql.service.OnlineUserMonitorService;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.BaseController;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.PasswordUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 系统用户管理
 */
@Api(tags = "系统用户管理")
@RestController
@RequestMapping("/user")
public class BaseUserController extends BaseController {

    @Autowired
    private BaseUserBusiness baseUserBusiness;
    @Autowired
    private BaseUserService baseUserService;
    @Autowired
    private BaseRoleService baseRoleService;
    @Autowired
    private BaseAccountService baseAccountService;
    @Autowired
    private BaseUserOrgService baseUserOrgService;
    @Autowired
    private OnlineUserMonitorService onlineUserMonitorService;

    @ApiOperation(value = "用户列表")
    @GetMapping
    public ResultBody<?> listUsers(@ModelAttribute BaseUserForm form) {
        final BaseUserForm query = form != null ? form : new BaseUserForm();
        applyDateRange(query);
        if (StrUtil.isNotBlank(query.getMobile())) {
            return ResultBody.callback(() -> baseUserService.getUserByPhone(query.getMobile()));
        }
        if (query.getPageForm() != null
                && (query.getPageForm().getCurrPage() != null || query.getPageForm().getPageSize() != null)) {
            PageForm pageForm = query.getPageForm();
            return ResultBody.callback(() -> baseUserBusiness.selectEntitys(query, pageForm));
        }
        if (hasListCriteria(query)) {
            return ResultBody.callback(() -> baseUserBusiness.selectEntitys(query));
        }
        return ResultBody.callback(() -> baseUserBusiness.findListPage(query));
    }

    @ApiOperation(value = "按关键字检索用户")
    @GetMapping(params = "keyword")
    public ResultBody<List<BaseUser>> searchUsers(@RequestParam String keyword) {
        return ResultBody.callback(() -> baseUserBusiness.retrievalUsers(keyword));
    }

    @ApiOperation(value = "按手机号查询用户")
    @GetMapping(params = "phone")
    public ResultBody<BaseUser> getUserByPhone(@RequestParam String phone) {
        return ResultBody.callback(() -> baseUserService.getUserByPhone(phone));
    }

    @ApiOperation(value = "按 ID 批量查询用户")
    @GetMapping(params = "ids")
    public ResultBody<List<BaseUser>> getUsersByIds(@RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(Long::valueOf)
                .collect(Collectors.toList());
        return ResultBody.callback(() -> baseUserService.getUsersByIds(idList));
    }

    @ApiOperation(value = "全部用户")
    @GetMapping("/all")
    public ResultBody<List<BaseUser>> listAllUsers() {
        return ResultBody.callback(() -> baseUserService.findAllList());
    }

    @ApiOperation(value = "用户统计")
    @GetMapping("/statistics")
    public ResultBody<UserInfoStatistics> getUserStatistics() {
        return ResultBody.callback(() -> {
            UserInfoStatistics stats = new UserInfoStatistics();
            stats.setOnlineUser(countOnlineUsers());
            stats.setUsersTotal(baseUserService.count(new BaseUser()));
            return stats;
        });
    }

    private long countOnlineUsers() {
        return onlineUserMonitorService.countOnlineUsers(new OnlineUserFilter());
    }

    @ApiOperation(value = "用户详情")
    @GetMapping("/{userId}")
    public ResultBody<BaseUser> getUser(@PathVariable Long userId) {
        return ResultBody.callback(() -> baseUserService.selectById(userId));
    }

    @ApiOperation(value = "用户账号列表")
    @GetMapping("/{userId}/accounts")
    public ResultBody<List<BaseAccount>> getUserAccounts(@PathVariable Long userId) {
        return ResultBody.callback(() -> baseAccountService.getUserAccounts(userId));
    }

    @ApiOperation(value = "用户角色")
    @GetMapping("/{userId}/roles")
    public ResultBody<List<BaseRole>> getUserRoles(@PathVariable Long userId) {
        return ResultBody.callback(() -> baseUserBusiness.getUserRoles(userId));
    }

    @ApiOperation(value = "用户跨组织数据授权")
    @GetMapping("/{userId}/orgs")
    public ResultBody<List<BaseUserOrg>> getUserOrgs(@PathVariable Long userId) {
        return ResultBody.callback(() -> baseUserBusiness.getUserOrgs(userId));
    }

    @ApiOperation(value = "创建用户")
    @OperatorLog
    @PostMapping
    public ResultBody<Void> createUser(@RequestBody BaseUserForm form) {
        baseUserBusiness.addUser(form);
        if (form.getUserId() != null && form.getOrgIds() != null) {
            baseUserOrgService.saveUserOrgs(form.getUserId(), form.getOrgIds());
        }
        return ResultBody.ok();
    }

    @ApiOperation(value = "保存用户（含角色）")
    @OperatorLog
    @PutMapping("/{userId}")
    public ResultBody<BaseUser> saveUser(@PathVariable Long userId, @RequestBody BaseUserForm form) {
        form.setUserId(userId);
        return ResultBody.callback("保存用户信息成功", () -> {
            BaseUser entity = baseUserBusiness.saveEntity(form);
            if (ObjectUtil.isNotEmpty(form.getRoleIds())) {
                baseRoleService.saveUserRoles(entity.getUserId(), form.getRoleIds());
            }
            if (form.getOrgIds() != null) {
                baseUserOrgService.saveUserOrgs(entity.getUserId(), form.getOrgIds());
            }
            return entity;
        });
    }

    @ApiOperation(value = "更新用户")
    @PatchMapping("/{userId}")
    public ResultBody<Void> patchUser(@PathVariable Long userId, @RequestBody BaseUserForm form) {
        form.setUserId(userId);
        baseUserBusiness.updateUser(form);
        if (form.getOrgIds() != null) {
            baseUserOrgService.saveUserOrgs(userId, form.getOrgIds());
        }
        return ResultBody.ok();
    }

    @ApiOperation(value = "注册用户")
    @PostMapping("/registrations")
    public ResultBody<?> register(
            @RequestParam(value = "registerIp", required = false) String registerIp,
            @RequestParam("userName") String userName,
            @RequestParam(value = "nickName", required = false) String nickName,
            @RequestParam(value = "accountType", required = false) String accountType,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword) {
        try {
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
        } catch (ValidateException | ServiceException e) {
            return ResultBody.failed().msg(e.getMessage());
        } catch (Exception e) {
            return ResultBody.failed().msg("帐号注册错误").exception(e);
        }
    }

    @ApiOperation(value = "注销用户")
    @PostMapping("/{userId}/closure")
    public ResultBody<Boolean> closeUser(@PathVariable Long userId, @RequestBody(required = false) BaseUserForm form) {
        BaseUser baseUser = form != null ? form : new BaseUser();
        baseUser.setUserId(userId);
        return ResultBody.callback(() -> baseUserBusiness.close(baseUser));
    }

    @ApiOperation(value = "登录")
    @PostMapping("/sessions")
    public ResultBody<UserAccount> createSession(
            @RequestParam String username,
            @RequestParam(required = false) String loginType) {
        if (StrUtil.isNotBlank(loginType)) {
            return ResultBody.callback(() -> baseUserBusiness.login(username, loginType));
        }
        return ResultBody.callback(() -> baseUserBusiness.login(username));
    }

    @ApiOperation(value = "移动端注册并登录")
    @PostMapping("/sessions/mobile")
    public ResultBody<UserAccount> createMobileSession(@RequestBody ThirdPartyUserForm form) {
        return ResultBody.callback(() -> baseUserBusiness.loginAndRegisterMobileUser(form));
    }

    @ApiOperation(value = "更新密码")
    @PutMapping("/{userId}/password")
    public ResultBody<?> updatePassword(@PathVariable Long userId, @RequestBody BaseUserForm form) {
        baseUserBusiness.updatePassword(userId, form.getPassword());
        return ResultBody.ok().msg("修改密码成功");
    }

    @ApiOperation(value = "更新状态")
    @PatchMapping("/{userId}/status")
    public ResultBody<BaseUser> patchUserStatus(@PathVariable Long userId, @RequestBody BaseUserForm form) {
        return ResultBody.callback(() -> {
            BaseUser baseUser = new BaseUser();
            baseUser.setUserId(userId);
            baseUser.setStatus(form.getStatus());
            baseUserBusiness.updateUser(baseUser);
            return baseUser;
        });
    }

    @ApiOperation(value = "激活 Email 账号")
    @PutMapping("/{userId}/activations/email")
    public ResultBody<?> activateEmail(@PathVariable Long userId) {
        BaseUser baseUser = new BaseUser();
        baseUser.setUserId(userId);
        baseUserBusiness.activationEmailAccount(baseUser);
        return ResultBody.ok().msg("激活用户Email帐号成功");
    }

    @ApiOperation(value = "激活手机账号")
    @PutMapping("/{userId}/activations/mobile")
    public ResultBody<?> activateMobile(@PathVariable Long userId) {
        BaseUser baseUser = new BaseUser();
        baseUser.setUserId(userId);
        baseUserBusiness.activationMobileAccount(baseUser);
        return ResultBody.ok().msg("激活用户手机帐号成功");
    }

    @ApiOperation(value = "分配角色")
    @PutMapping("/{userId}/roles")
    public ResultBody<Void> putUserRoles(@PathVariable Long userId, @RequestBody BaseUserForm form) {
        baseRoleService.saveUserRoles(userId, form.getRoleIds());
        return ResultBody.ok();
    }

    @ApiOperation(value = "分配跨组织数据授权")
    @PutMapping("/{userId}/orgs")
    public ResultBody<Void> putUserOrgs(@PathVariable Long userId, @RequestBody BaseUserForm form) {
        baseUserOrgService.saveUserOrgs(userId, form.getOrgIds());
        return ResultBody.ok();
    }

    @ApiOperation(value = "第三方账号")
    @PostMapping("/third-party-accounts")
    public ResultBody<Void> createThirdPartyAccount(
            @RequestParam("account") String account,
            @RequestParam("password") String password,
            @RequestParam("accountType") String accountType,
            @RequestParam("nickName") String nickName,
            @RequestParam("avatar") String avatar) {
        BaseUser user = new BaseUser();
        user.setNickName(nickName);
        user.setUserName(account);
        user.setPassword(password);
        user.setAvatar(avatar);
        baseUserBusiness.addUserThirdParty(user, accountType);
        return ResultBody.ok();
    }

    @ApiOperation(value = "绑定第三方账号（按手机号）")
    @PostMapping("/third-party-account-bindings")
    public ResultBody<Void> bindThirdPartyAccount(
            @RequestParam String account,
            @RequestParam String password,
            @RequestParam String accountType,
            @RequestParam String phone) {
        BaseAccount baseAccount = new BaseAccount();
        baseAccount.setAccount(account);
        baseAccount.setPassword(password);
        baseAccount.setAccountType(accountType);
        baseUserBusiness.bindUserThirdPartyByPhone(phone, baseAccount);
        return ResultBody.ok();
    }

    @ApiOperation(value = "更新 OpenId（按手机号）")
    @PutMapping("/accounts/open-id")
    public ResultBody<BaseUser> putOpenIdByPhone(
            @RequestParam String openId,
            @RequestParam String sessionKey,
            @RequestParam String accountType,
            @RequestParam String phone) {
        return ResultBody.callback(() ->
                baseAccountService.updateOpenIdByPhone(openId, sessionKey, accountType, phone));
    }

    private void applyDateRange(BaseUserForm form) {
        if (ObjectUtil.isNotEmpty(form.getDateRange())) {
            form.setBeginTime(form.getDateRange()[0]);
            form.setEndTime(form.getDateRange()[1]);
        }
    }

    private boolean hasListCriteria(BaseUserForm form) {
        return form.getBeginTime() != null || form.getEndTime() != null
                || StrUtil.isNotBlank(form.getUserName()) || StrUtil.isNotBlank(form.getNickName())
                || form.getStatus() != null || form.getCompanyId() != null;
    }
}
