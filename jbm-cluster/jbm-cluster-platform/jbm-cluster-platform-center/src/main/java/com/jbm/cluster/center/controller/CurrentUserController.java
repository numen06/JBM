package com.jbm.cluster.center.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.auth.AuthorityMenu;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.center.business.BaseUserBusiness;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.common.mysql.service.BaseUserService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.util.PasswordUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 当前登录用户
 */
@Api(tags = "当前登陆用户")
@RestController
@RequestMapping("/current")
public class CurrentUserController {

    @Autowired
    private BaseUserBusiness baseUserBusiness;
    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private BaseUserService baseUserService;

    @SaCheckLogin
    @ApiOperation(value = "当前用户")
    @GetMapping("/user")
    public ResultBody<UserAccount> getCurrentUser() {
        return ResultBody.callback(() -> baseUserBusiness.getUserAccount(LoginHelper.getUserId()));
    }

    @SaCheckLogin
    @ApiOperation(value = "当前用户菜单")
    @GetMapping("/user/menus")
    public ResultBody<List<AuthorityMenu>> listCurrentUserMenus() {
        return ResultBody.callback(() -> {
            JbmLoginUser loginUser = LoginHelper.softGetLoginUser();
            Long userId = resolveUserId(loginUser);
            Long appId = resolveAppId(loginUser);
            boolean fullMenu = LoginHelper.isAdmin(userId);
            if (!fullMenu && userId != null) {
                BaseUser baseUser = baseUserService.getUserById(userId);
                fullMenu = baseUser != null
                        && JbmConstants.isSuperUser(baseUser.getUserId(), baseUser.getUserName(), baseUser.getUserType());
            }
            return baseAuthorityService.findAuthorityMenuByUser(userId, appId, fullMenu);
        });
    }

    private Long resolveUserId(JbmLoginUser loginUser) {
        if (loginUser != null && loginUser.getUserId() != null) {
            return loginUser.getUserId();
        }
        return LoginHelper.getUserId();
    }

    private Long resolveAppId(JbmLoginUser loginUser) {
        if (loginUser != null && loginUser.getAppId() != null) {
            return loginUser.getAppId();
        }
        try {
            List<String> parts = StrUtil.split(StpUtil.getLoginIdAsString(), LoginHelper.JOIN_CODE);
            if (parts.size() >= 3) {
                String appId = parts.get(parts.size() - 2);
                if (StrUtil.isNotBlank(appId)) {
                    return Long.parseLong(appId);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @SaCheckLogin
    @ApiOperation(value = "更新当前用户")
    @PutMapping("/user")
    public ResultBody<Void> updateCurrentUser(@RequestBody BaseUserForm form) {
        JbmLoginUser loginUser = SecurityUtils.getLoginUser();
        BaseUser user = new BaseUser();
        user.setUserId(loginUser.getUserId());
        if (StrUtil.isNotBlank(form.getNickName())) {
            user.setNickName(form.getNickName());
        }
        if (StrUtil.isNotBlank(form.getUserDesc())) {
            user.setUserDesc(form.getUserDesc());
        }
        if (StrUtil.isNotBlank(form.getAvatar())) {
            user.setAvatar(form.getAvatar());
        }
        if (StrUtil.isNotBlank(form.getRealName())) {
            user.setRealName(form.getRealName());
        }
        baseUserBusiness.updateUser(user);
        return ResultBody.ok();
    }

    @SaCheckLogin
    @ApiOperation(value = "更新当前用户密码")
    @PutMapping("/user/password")
    public ResultBody<Void> updateCurrentUserPassword(@RequestBody BaseUserForm form) {
        Long userId = SecurityUtils.getLoginUser().getUserId();
        try {
            PasswordUtils.validatorPassword(
                    form.getOriginPassword(), form.getCurrentPassword(), form.getConfirmPassword());
        } catch (Exception e) {
            throw new ServiceException(e);
        }
        baseUserBusiness.updatePassword(userId, form.getCurrentPassword());
        return ResultBody.ok();
    }
}
