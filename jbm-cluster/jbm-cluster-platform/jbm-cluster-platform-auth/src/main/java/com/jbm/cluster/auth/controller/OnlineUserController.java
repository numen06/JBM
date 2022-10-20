package com.jbm.cluster.auth.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.model.auth.SysUserOnline;
import com.jbm.cluster.auth.form.OnlineUserSearchForm;
import com.jbm.cluster.core.constant.JbmCacheConstants;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Created wesley.zhang
 * @Date 2022/5/4 13:36
 * @Description TODO
 */
@Api(value = "在线用户监控", tags = {"在线用户监控管理"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/online")
public class OnlineUserController {

    @Autowired
    private RedisService redisService;

    @ApiOperation("在线用户列表")
//    @SaCheckPermission("monitor:online:list")
    @PostMapping("/pageList")
    public ResultBody<DataPaging<SysUserOnline>> pageList(@RequestBody OnlineUserSearchForm onlineUserSearchForm) {
        // 获取所有未过期的 token
        List<String> keys = StpUtil.searchTokenValue("", -1, 0);
        List<SysUserOnline> userOnlineList = new ArrayList<SysUserOnline>();
        for (String key : keys) {
            String token = key.replace(JbmCacheConstants.LOGIN_TOKEN_KEY, "");
            // 如果已经过期则踢下线
            Long activityTime = StpUtil.stpLogic.getTokenActivityTimeoutByToken(token);
            if (activityTime < 0) {
                continue;
            }
            SysUserOnline sysUserOnline = redisService.getCacheObject(JbmCacheConstants.ONLINE_TOKEN_KEY + token);
            sysUserOnline.setActivityTime(DateUtil.offset(DateTime.now(), DateField.SECOND, activityTime.intValue()));
            userOnlineList.add(sysUserOnline);
        }
        if (StrUtil.isNotEmpty(onlineUserSearchForm.getIpaddr()) && StrUtil.isNotEmpty(onlineUserSearchForm.getUserName())) {
            userOnlineList = userOnlineList.stream().filter(userOnline ->
                    StrUtil.equals(onlineUserSearchForm.getIpaddr(), userOnline.getIpaddr()) &&
                            StrUtil.equals(onlineUserSearchForm.getUserName(), userOnline.getUserName())
            ).collect(Collectors.toList());
        } else if (StrUtil.isNotEmpty(onlineUserSearchForm.getIpaddr())) {
            userOnlineList = userOnlineList.stream().filter(userOnline ->
                            StrUtil.equals(onlineUserSearchForm.getIpaddr(), userOnline.getIpaddr()))
                    .collect(Collectors.toList());
        } else if (StrUtil.isNotEmpty(onlineUserSearchForm.getUserName())) {
            userOnlineList = userOnlineList.stream().filter(userOnline ->
                    StrUtil.equals(onlineUserSearchForm.getUserName(), userOnline.getUserName())
            ).collect(Collectors.toList());
        }
        Collections.reverse(userOnlineList);
        userOnlineList.removeAll(Collections.singleton(null));
        return ResultBody.ok(new DataPaging<SysUserOnline>(userOnlineList, userOnlineList.size()));
    }

    @ApiOperation("踢出用户")
    @SaCheckPermission("ACTION_monitor:online:forceLogout")
    @DeleteMapping("/kickout/{tokenId}")
    public ResultBody<Void> forceLogout(@PathVariable String tokenId) {
        try {
            SaOAuth2Util.revokeAccessToken(tokenId);
            StpUtil.kickoutByTokenValue(tokenId);
        } catch (NotLoginException e) {
        }
        return ResultBody.ok();
    }

    @ApiOperation("注销用户")
    @SaCheckPermission("ACTION_monitor:online:logout")
    @DeleteMapping("/logout/{tokenId}")
    public ResultBody<Void> logout(@PathVariable String tokenId) {
        try {
            SaOAuth2Util.revokeAccessToken(tokenId);
            StpUtil.logoutByTokenValue(tokenId);
        } catch (NotLoginException e) {
        }
        return ResultBody.ok();
    }

    @ApiOperation("刷新Token临时有效期")
    @DeleteMapping("/refresh")
    public ResultBody<Void> refresh() {
        return ResultBody.callback("刷新成功", () -> {
            StpUtil.updateLastActivityToNow();
            return null;
        });
    }
}
