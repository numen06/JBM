package com.jbm.cluster.center.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.constants.OrgUserScope;
import com.jbm.cluster.api.entitys.auth.AuthorityMenu;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.api.model.basic.OrgUserQueryResult;
import com.jbm.cluster.center.service.BaseAuthorityService;
import com.jbm.cluster.center.service.BaseUserService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.PasswordUtils;
import com.jbm.util.sensitive.SensitiveContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author: wesley.zhang
 * @date: 2019/5/24 13:31
 * @description:
 */
@Api(tags = "当前登陆用户")
@RestController
@RequestMapping("/current")
public class CurrentUserController {

    @Autowired
    private BaseUserService baseUserService;
    @Autowired
    private BaseAuthorityService baseAuthorityService;

    /**
     * 修改当前登录用户密码
     *
     * @return
     */
    @SaCheckLogin
    @ApiOperation(value = "修改当前登录用户密码", notes = "修改当前登录用户密码")
    @GetMapping("/user/rest/password")
    public ResultBody restPassword(@RequestParam(value = "password") String password) {
        baseUserService.updatePassword(SecurityUtils.getLoginUser().getUserId(), password);
        return ResultBody.ok();
    }

    @SaCheckLogin
    @ApiOperation(value = "修改当前用户密码2", notes = "修改用户密码")
    @PostMapping("/user/update/password")
    public ResultBody updatePassword(@RequestBody BaseUserForm baseUserForm) {
        Long userId = SecurityUtils.getLoginUser().getUserId();
        try {
            PasswordUtils.validatorPassword(baseUserForm.getOriginPassword(), baseUserForm.getCurrentPassword(), baseUserForm.getConfirmPassword());
        } catch (Exception e) {
            throw new ServiceException(e);
        }
        baseUserService.updatePassword(userId, baseUserForm.getCurrentPassword());
        return ResultBody.ok();
    }

    @SaCheckLogin
    @ApiOperation(value = "当前账户权限信息", notes = "当前账户权限信息，不返回 userId")
    @GetMapping("/user/account")
    public ResultBody<UserAccount> userAccount() {
        try {
            SensitiveContext.skipMask();
            return ResultBody.callback(() -> {
                UserAccount userAccount = baseUserService.getUserAccount(LoginHelper.getUserId());
                userAccount.setUserId(null);
                return userAccount;
            });
        } finally {
            SensitiveContext.clear();
        }
    }

    @SaCheckLogin
    @ApiOperation(value = "获取当前登录用户信息", notes = "返回与 /user/model 一致的 BaseUser，无需传参，不返回 userId")
    @GetMapping("/user/model")
    public ResultBody<BaseUser> currentUserModel() {
        try {
            SensitiveContext.skipMask();
            return ResultBody.callback(() -> {
                BaseUser user = baseUserService.getUserById(LoginHelper.getUserId());
                user.setUserId(null);
                return user;
            });
        } finally {
            SensitiveContext.clear();
        }
    }

    /**
     * 修改当前登录用户基本信息
     *
     * @param nickName
     * @param userDesc
     * @param avatar
     * @return
     */
    @SaCheckLogin
    @ApiOperation(value = "修改当前登录用户基本信息", notes = "修改当前登录用户基本信息")
    @PostMapping("/user/update")
    public ResultBody updateUserInfo(
            @RequestParam(value = "nickName") String nickName,
            @RequestParam(value = "userDesc", required = false) String userDesc,
            @RequestParam(value = "avatar", required = false) String avatar,
            @RequestParam(value = "realName", required = false) String realName
    ) {
        JbmLoginUser openUserDetails = SecurityUtils.getLoginUser();
        BaseUser user = new BaseUser();
        user.setUserId(openUserDetails.getUserId());
        if (StrUtil.isNotBlank(nickName)) {
            user.setNickName(nickName);
        }
        if (StrUtil.isNotBlank(userDesc)) {
            user.setUserDesc(userDesc);
        }
        if (StrUtil.isNotBlank(avatar)) {
            user.setAvatar(avatar);
        }
        if (StrUtil.isNotBlank(realName)) {
            user.setRealName(realName);
        }
        baseUserService.updateUser(user);
//        if (StrUtil.isNotBlank(nickName))
//            openUserDetails.setNickName(nickName);
//        if (StrUtil.isNotBlank(avatar))
//            openUserDetails.setAvatar(avatar);
//        if (StrUtil.isNotBlank(realName))
//            openUserDetails.setRealName(realName);
//        SecurityUtils.updateLoginUser(redisTokenStore, openUserDetails);
        return ResultBody.ok();
    }

    /**
     * 获取登陆用户已分配权限
     *
     * @return
     */
    @SaCheckLogin
    @ApiOperation(value = "获取当前登录用户已分配菜单权限", notes = "获取当前登录用户已分配菜单权限")
    @GetMapping("/user/menu")
    public ResultBody<List<AuthorityMenu>> findAuthorityMenu() {
        JbmLoginUser loginUser = SecurityUtils.getLoginUser();
        boolean fullMenu = LoginHelper.isAdmin() || JbmConstants.ROOT.equals(loginUser.getUsername());
        List<AuthorityMenu> result = baseAuthorityService.findAuthorityMenuByUser(loginUser.getUserId(), loginUser.getAppId(), fullMenu);
        return ResultBody.callback(() -> result);
    }

    @SaCheckLogin
    @ApiOperation(value = "获取同公司用户列表", notes = "返回用户列表及部门-用户树")
    @GetMapping("/org/users/company")
    public ResultBody<OrgUserQueryResult> listCompanyUsers(
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "realName", required = false) String realName,
            @RequestParam(value = "mobile", required = false) String mobile,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        BaseUserForm form = buildUserForm(userName, realName, mobile, status);
        return ResultBody.callback(() -> baseUserService.selectOrgUsersWithTree(OrgUserScope.COMPANY, form));
    }

    @SaCheckLogin
    @ApiOperation(value = "获取同公司用户分页列表", notes = "基于当前登录人所属顶层公司分页查询用户")
    @PostMapping("/org/users/company/pageList")
    public ResultBody<DataPaging<BaseUser>> pageCompanyUsers(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        return pageOrgUsers(OrgUserScope.COMPANY, pageRequestBody);
    }

    @SaCheckLogin
    @ApiOperation(value = "获取同部门用户列表", notes = "返回用户列表及部门-用户树")
    @GetMapping("/org/users/department")
    public ResultBody<OrgUserQueryResult> listDepartmentUsers(
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "realName", required = false) String realName,
            @RequestParam(value = "mobile", required = false) String mobile,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        BaseUserForm form = buildUserForm(userName, realName, mobile, status);
        return ResultBody.callback(() -> baseUserService.selectOrgUsersWithTree(OrgUserScope.DEPARTMENT, form));
    }

    @SaCheckLogin
    @ApiOperation(value = "获取同部门用户分页列表", notes = "基于当前登录人所属部门精确匹配分页查询用户")
    @PostMapping("/org/users/department/pageList")
    public ResultBody<DataPaging<BaseUser>> pageDepartmentUsers(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        return pageOrgUsers(OrgUserScope.DEPARTMENT, pageRequestBody);
    }

    @SaCheckLogin
    @ApiOperation(value = "获取同部门及子部门用户列表", notes = "返回用户列表及部门-用户树")
    @GetMapping("/org/users/departmentTree")
    public ResultBody<OrgUserQueryResult> listDepartmentTreeUsers(
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "realName", required = false) String realName,
            @RequestParam(value = "mobile", required = false) String mobile,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        BaseUserForm form = buildUserForm(userName, realName, mobile, status);
        return ResultBody.callback(() -> baseUserService.selectOrgUsersWithTree(OrgUserScope.DEPARTMENT_TREE, form));
    }

    @SaCheckLogin
    @ApiOperation(value = "获取同部门及子部门用户分页列表", notes = "基于当前登录人所属部门及下级组织分页查询用户")
    @PostMapping("/org/users/departmentTree/pageList")
    public ResultBody<DataPaging<BaseUser>> pageDepartmentTreeUsers(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        return pageOrgUsers(OrgUserScope.DEPARTMENT_TREE, pageRequestBody);
    }

    private ResultBody<DataPaging<BaseUser>> pageOrgUsers(OrgUserScope scope, PageRequestBody pageRequestBody) {
        return ResultBody.callback(() -> {
            PageRequestBody requestBody = ObjectUtil.defaultIfNull(pageRequestBody, new PageRequestBody());
            BaseUserForm form = requestBody.tryGet(BaseUserForm.class);
            if (ObjectUtil.isNotEmpty(form.getDateRange())) {
                form.setBeginTime(form.getDateRange()[0]);
                form.setEndTime(form.getDateRange()[1]);
            }
            PageForm pageForm = requestBody.getPageForm();
            return baseUserService.selectOrgUsers(scope, form, pageForm);
        });
    }

    private BaseUserForm buildUserForm(String userName, String realName, String mobile, Integer status) {
        BaseUserForm form = new BaseUserForm();
        form.setUserName(userName);
        form.setRealName(realName);
        form.setMobile(mobile);
        form.setStatus(status);
        return form;
    }
}
