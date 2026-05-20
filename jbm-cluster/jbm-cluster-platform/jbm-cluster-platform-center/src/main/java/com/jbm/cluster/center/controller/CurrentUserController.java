package com.jbm.cluster.center.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.auth.AuthorityMenu;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.center.business.BaseAuthorityBusiness;
import com.jbm.cluster.center.business.BaseUserBusiness;
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
    private BaseAuthorityBusiness baseAuthorityBusiness;

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
        JbmLoginUser loginUser = SecurityUtils.getLoginUser();
        boolean fullMenu = LoginHelper.isAdmin() || JbmConstants.ROOT.equals(loginUser.getUsername());
        return ResultBody.callback(() -> baseAuthorityBusiness.findAuthorityMenuByUser(
                loginUser.getUserId(), loginUser.getAppId(), fullMenu));
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
