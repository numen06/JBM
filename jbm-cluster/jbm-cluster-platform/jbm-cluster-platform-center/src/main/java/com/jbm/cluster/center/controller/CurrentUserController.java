package com.jbm.cluster.center.controller;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.cluster.api.entitys.auth.AuthorityMenu;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.center.service.BaseAuthorityService;
import com.jbm.cluster.center.service.BaseUserService;
import com.jbm.cluster.common.constants.CommonConstants;
import com.jbm.cluster.common.security.JbmClusterHelper;
import com.jbm.cluster.common.security.OpenUserDetails;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.util.PasswordUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author: wesley.zhang
 * @date: 2019/5/24 13:31
 * @description:
 */
@Api(tags = "当前登陆用户")
@RestController
public class CurrentUserController {

    @Autowired
    private BaseUserService baseUserService;
    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private RedisTokenStore redisTokenStore;

    /**
     * 修改当前登录用户密码
     *
     * @return
     */
    @ApiOperation(value = "修改当前登录用户密码", notes = "修改当前登录用户密码")
    @GetMapping("/current/user/rest/password")
    public ResultBody restPassword(@RequestParam(value = "password") String password) {
        baseUserService.updatePassword(JbmClusterHelper.getUser().getUserId(), password);
        return ResultBody.ok();
    }

    @ApiOperation(value = "修改当前用户密码2", notes = "修改用户密码")
    @PostMapping("/current/user/update/password")
    public ResultBody updatePassword(@RequestBody BaseUserForm baseUserForm) {
        Long userId = JbmClusterHelper.getUser().getUserId();
        try {
            PasswordUtils.validatorPassword(baseUserForm.getOriginPassword(), baseUserForm.getCurrentPassword(), baseUserForm.getConfirmPassword());
        } catch (Exception e) {
            throw new ServiceException(e);
        }
        baseUserService.updatePassword(userId, baseUserForm.getCurrentPassword());
        return ResultBody.ok();
    }

    /**
     * 修改当前登录用户基本信息
     *
     * @param nickName
     * @param userDesc
     * @param avatar
     * @return
     */
    @ApiOperation(value = "修改当前登录用户基本信息", notes = "修改当前登录用户基本信息")
    @PostMapping("/current/user/update")
    public ResultBody updateUserInfo(
            @RequestParam(value = "nickName") String nickName,
            @RequestParam(value = "userDesc", required = false) String userDesc,
            @RequestParam(value = "avatar", required = false) String avatar,
            @RequestParam(value = "realName", required = false) String realName
    ) {
        OpenUserDetails openUserDetails = JbmClusterHelper.getUser();
        BaseUser user = new BaseUser();
        user.setUserId(openUserDetails.getUserId());
        if (StrUtil.isNotBlank(nickName))
            user.setNickName(nickName);
        if (StrUtil.isNotBlank(userDesc))
            user.setUserDesc(userDesc);
        if (StrUtil.isNotBlank(avatar))
            user.setAvatar(avatar);
        if (StrUtil.isNotBlank(realName))
            user.setRealName(realName);
        baseUserService.updateUser(user);
        if (StrUtil.isNotBlank(nickName))
            openUserDetails.setNickName(nickName);
        if (StrUtil.isNotBlank(avatar))
            openUserDetails.setAvatar(avatar);
        if (StrUtil.isNotBlank(realName))
            openUserDetails.setRealName(realName);
        JbmClusterHelper.updateOpenUser(redisTokenStore, openUserDetails);
        return ResultBody.ok();
    }

    /**
     * 获取登陆用户已分配权限
     *
     * @return
     */
    @ApiOperation(value = "获取当前登录用户已分配菜单权限", notes = "获取当前登录用户已分配菜单权限")
    @GetMapping("/current/user/menu")
    public ResultBody<List<AuthorityMenu>> findAuthorityMenu() {
        List<AuthorityMenu> result = baseAuthorityService.findAuthorityMenuByUser(JbmClusterHelper.getUser().getUserId(), CommonConstants.ROOT.equals(JbmClusterHelper.getUser().getUsername()));
        return ResultBody.ok().data(result);
    }
}
