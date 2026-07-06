package com.jbm.cluster.auth.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.SaManager;
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
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    private RedisService redisService;

    @ApiOperation("在线用户列表")
    @SaCheckPermission("ACTION_monitor:online:list")
    @PostMapping("/pageList")
    public ResultBody<DataPaging<SysUserOnline>> pageList(@RequestBody OnlineUserSearchForm onlineUserSearchForm) {
        List<SysUserOnline> userOnlineList = new ArrayList<SysUserOnline>();
        
        // 方法1：直接从Redis获取所有在线用户信息
        Collection<String> onlineKeys = redisService.keys(JbmCacheConstants.ONLINE_TOKEN_KEY + "*");
        for (String onlineKey : onlineKeys) {
            try {
                SysUserOnline sysUserOnline = redisService.getCacheObject(onlineKey);
                if (sysUserOnline == null) {
                    continue;
                }
                
                // 获取token值用于检查活动超时时间和过期时间
                String token = onlineKey.replace(JbmCacheConstants.ONLINE_TOKEN_KEY, "");
                
                // 检查token是否还有效并设置相关时间
                try {
                    // 设置临时有效期（活动超时时间）
                    Long activityTimeout = StpUtil.stpLogic.getTokenActivityTimeoutByToken(token);
                    if (activityTimeout != null && activityTimeout > 0) {
                        sysUserOnline.setActivityTime(DateUtil.offset(DateTime.now(), DateField.SECOND, activityTimeout.intValue()));
                    }
                    
                    // 设置过期时间（从Redis获取在线用户key的过期时间）
                    Long expireTime = redisService.getExpire(onlineKey);
                    if (expireTime != null && expireTime > 0) {
                        sysUserOnline.setExpiredTime(DateUtil.offset(DateTime.now(), DateField.SECOND, expireTime.intValue()));
                    }
                } catch (Exception e) {
                    // token可能已失效，继续处理但不设置相关时间
                }
                
                userOnlineList.add(sysUserOnline);
            } catch (Exception e) {
                // 某个在线用户数据异常，跳过继续处理其他用户
                continue;
            }
        }
        
        // 根据搜索条件过滤
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
        
        // 获取分页参数
        PageForm pageForm = onlineUserSearchForm.getPageForm();
        if (pageForm == null) {
            pageForm = new PageForm();
        }
        
        // 计算总记录数
        int total = userOnlineList.size();
        
        // 计算总页数
        int pageSize = pageForm.getPageSize() != null ? pageForm.getPageSize() : Integer.MAX_VALUE;
        int currPage = pageForm.getCurrPage() != null ? pageForm.getCurrPage() : 1;
        long totalPage = (total + pageSize - 1) / pageSize;
        
        // 进行内存分页
        int fromIndex = (currPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        
        List<SysUserOnline> pagedList;
        if (fromIndex >= total) {
            // 当前页超出范围，返回空列表
            pagedList = new ArrayList<>();
        } else {
            pagedList = userOnlineList.subList(fromIndex, toIndex);
        }
        
        return ResultBody.ok(new DataPaging<SysUserOnline>(pagedList, (long) total, totalPage, pageForm));
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
    @SaCheckRole("admin")
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
        try {
            if (minutes == null || minutes <= 0) {
                return ResultBody.failed("过期时间必须大于0分钟");
            }
            
            // 计算新的过期时间（秒）
            long expireSeconds = minutes * 60L;
            
            try {
                // 方法1: 尝试通过Sa-Token的token值来设置过期
                String tokenValue = tokenId;
                
                // 检查token是否有效
                try {
                    Object loginId = StpUtil.getLoginIdByToken(tokenValue);
                    if (loginId != null) {
                        // 使用Sa-Token的API来设置token过期时间
                        // 通过修改token的活动超时来实现
                        StpUtil.updateLastActivityToNow();
                        
                        // 直接操作Redis设置token相关key的过期时间
                        // Sa-Token的token存储格式：
                        // sa:token:{tokenValue} - token信息
                        // sa:session:{tokenValue} - session信息
                        
                        String tokenPrefix = SaManager.getConfig().getTokenName();
                        if (StrUtil.isBlank(tokenPrefix)) {
                            tokenPrefix = "satoken";
                        }
                        String tokenKey = tokenPrefix + ":login:token:" + tokenValue;
                        String sessionKey = tokenPrefix + ":login:session:" + tokenValue;
                        String lastActivityKey = tokenPrefix + ":login:last-activity:" + tokenValue;
                        String oauth2AccessKey = tokenPrefix + ":oauth2:access-token:" + tokenValue;
                        
                        // 设置token key的过期时间（如果存在）
                        if (redisService.getExpire(tokenKey) > 0) {
                            redisService.expire(tokenKey, expireSeconds);
                        }
                        
                        // 设置session key的过期时间（如果存在）
                        if (redisService.getExpire(sessionKey) > 0) {
                            redisService.expire(sessionKey, expireSeconds);
                        }
                        // 调整活动超时 key
                        if (redisService.getExpire(lastActivityKey) > 0) {
                            redisService.expire(lastActivityKey, expireSeconds);
                        }
                        // 同步调整 OAuth2 access token key，避免双层 token 过期不一致
                        if (redisService.getExpire(oauth2AccessKey) > 0) {
                            redisService.expire(oauth2AccessKey, expireSeconds);
                        }
                        
                        log.info("Token已设置为{}分钟后过期: {}", minutes, tokenValue);
                        return ResultBody.ok("Token已设置为" + minutes + "分钟后过期");
                    }
                } catch (Exception e) {
                    // token可能已失效或不存在
                    log.warn("Token可能已失效: {}", tokenValue);
                }
                
                // 方法2: 如果是OAuth2 token，也尝试处理
                try {
                    SaOAuth2Util.revokeAccessToken(tokenValue);
                    // 重新设置一个短期有效的token
                    // 这里需要根据实际的OAuth2实现来处理
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
        try {
            // 调用expireToken方法，设置为1分钟过期（立即过期的最小单位）
            return expireToken(tokenId, 1);
        } catch (Exception e) {
            log.error("设置Token立即过期异常", e);
            return ResultBody.failed("设置Token立即过期异常: " + e.getMessage());
        }
    }
}
