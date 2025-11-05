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
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import lombok.RequiredArgsConstructor;
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
    @DeleteMapping("/refresh")
    public ResultBody<Void> refresh() {
        return ResultBody.callback("刷新成功", () -> {
            StpUtil.updateLastActivityToNow();
            return null;
        });
    }
}
