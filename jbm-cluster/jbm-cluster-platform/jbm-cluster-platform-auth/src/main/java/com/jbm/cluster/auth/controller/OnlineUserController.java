package com.jbm.cluster.auth.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.model.auth.SysUserOnline;
import com.jbm.cluster.auth.form.OnlineUserSearchForm;
import com.jbm.cluster.common.mysql.service.OnlineUserFilter;
import com.jbm.cluster.common.mysql.service.OnlineUserMonitorService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Created wesley.zhang
 * @Date 2022/5/4 13:36
 * @Description TODO
 */
@Slf4j
@Api(value = "在线用户监控", tags = {"在线用户监控管理"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/online")
public class OnlineUserController {

    @Autowired
    private OnlineUserMonitorService onlineUserMonitorService;
    @Autowired
    private RedisService redisService;

    @ApiOperation("在线用户列表")
//    @SaCheckPermission("monitor:online:list")
    @PostMapping("/pageList")
    public ResultBody<DataPaging<SysUserOnline>> pageList(@RequestBody OnlineUserSearchForm onlineUserSearchForm) {
        OnlineUserFilter filter = toFilter(onlineUserSearchForm);
        PageForm pageForm = onlineUserSearchForm.getPageForm();
        return ResultBody.ok(onlineUserMonitorService.pageList(filter, pageForm));
    }

    @ApiOperation("踢出用户")
    @SaCheckPermission("ACTION_monitor:online:forceLogout")
    @DeleteMapping("/kickout/{tokenId}")
    public ResultBody<Void> forceLogout(@PathVariable String tokenId) {
        onlineUserMonitorService.assertTokenAccessible(tokenId, new OnlineUserFilter());
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
        onlineUserMonitorService.assertTokenAccessible(tokenId, new OnlineUserFilter());
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

    @ApiOperation("设置Token指定时间过期")
    @PostMapping("/expire")
    public ResultBody<String> expireToken(@RequestParam String tokenId, @RequestParam Integer minutes) {
        onlineUserMonitorService.assertTokenAccessible(tokenId, new OnlineUserFilter());
        try {
            if (minutes == null || minutes <= 0) {
                return ResultBody.failed("过期时间必须大于0分钟");
            }

            long expireSeconds = minutes * 60L;

            try {
                String tokenValue = tokenId;

                try {
                    Object loginId = StpUtil.getLoginIdByToken(tokenValue);
                    if (loginId != null) {
                        StpUtil.updateLastActivityToNow();

                        String tokenPrefix = SaManager.getConfig().getTokenName();
                        if (StrUtil.isBlank(tokenPrefix)) {
                            tokenPrefix = "satoken";
                        }
                        String tokenKey = tokenPrefix + ":login:token:" + tokenValue;
                        String sessionKey = tokenPrefix + ":login:session:" + tokenValue;
                        String lastActivityKey = tokenPrefix + ":login:last-activity:" + tokenValue;
                        String oauth2AccessKey = tokenPrefix + ":oauth2:access-token:" + tokenValue;

                        if (redisService.getExpire(tokenKey) > 0) {
                            redisService.expire(tokenKey, expireSeconds);
                        }

                        if (redisService.getExpire(sessionKey) > 0) {
                            redisService.expire(sessionKey, expireSeconds);
                        }
                        if (redisService.getExpire(lastActivityKey) > 0) {
                            redisService.expire(lastActivityKey, expireSeconds);
                        }
                        if (redisService.getExpire(oauth2AccessKey) > 0) {
                            redisService.expire(oauth2AccessKey, expireSeconds);
                        }

                        log.info("Token已设置为{}分钟后过期: {}", minutes, tokenValue);
                        return ResultBody.ok("Token已设置为" + minutes + "分钟后过期");
                    }
                } catch (Exception e) {
                    log.warn("Token可能已失效: {}", tokenValue);
                }

                try {
                    SaOAuth2Util.revokeAccessToken(tokenValue);
                    log.info("OAuth2 Token已撤销: {}", tokenValue);
                    return ResultBody.ok("Token已设置为立即过期");
                } catch (Exception e) {
                    log.warn("OAuth2 token处理失败: {}", e.getMessage());
                }

                return ResultBody.failed("Token不存在或已失效");

            } catch (Exception e) {
                log.error("设置Token过期失败", e);
                return ResultBody.failed("设置Token过期失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("设置Token过期异常", e);
            return ResultBody.failed("设置Token过期异常: " + e.getMessage());
        }
    }

    @ApiOperation("设置Token立即过期")
    @PostMapping("/expireImmediately")
    public ResultBody<String> expireTokenImmediately(@RequestParam String tokenId) {
        onlineUserMonitorService.assertTokenAccessible(tokenId, new OnlineUserFilter());
        try {
            return expireToken(tokenId, 1);
        } catch (Exception e) {
            log.error("设置Token立即过期异常", e);
            return ResultBody.failed("设置Token立即过期异常: " + e.getMessage());
        }
    }

    private OnlineUserFilter toFilter(OnlineUserSearchForm form) {
        OnlineUserFilter filter = new OnlineUserFilter();
        if (form == null) {
            return filter;
        }
        filter.setIpaddr(form.getIpaddr());
        filter.setUserName(form.getUserName());
        filter.setAppId(form.getAppId());
        filter.setCompanyId(form.getCompanyId());
        return filter;
    }
}
