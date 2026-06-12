package com.jbm.cluster.common.mysql.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.model.auth.SysUserOnline;
import com.jbm.cluster.core.constant.JbmCacheConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在线用户 Redis 读取、字段补全与数据范围过滤。
 */
@Service
public class OnlineUserMonitorService {

    @Autowired
    private RedisService redisService;
    @Autowired
    private OnlineUserScopeHelper onlineUserScopeHelper;
    @Autowired
    private BaseUserService baseUserService;

    public DataPaging<SysUserOnline> pageList(OnlineUserFilter filter, PageForm pageForm) {
        OnlineUserVisibleScope scope = onlineUserScopeHelper.resolveScope(filter);
        List<SysUserOnline> visible = loadVisibleOnlineUsers(scope, filter);
        if (pageForm == null) {
            pageForm = new PageForm();
        }
        int total = visible.size();
        int pageSize = pageForm.getPageSize() != null ? pageForm.getPageSize() : Integer.MAX_VALUE;
        int currPage = pageForm.getCurrPage() != null ? pageForm.getCurrPage() : 1;
        long totalPage = pageSize == 0 ? 0 : (total + pageSize - 1L) / pageSize;
        int fromIndex = (currPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<SysUserOnline> pagedList;
        if (fromIndex >= total) {
            pagedList = new ArrayList<>();
        } else {
            pagedList = visible.subList(fromIndex, toIndex);
        }
        return new DataPaging<>(pagedList, (long) total, totalPage, pageForm);
    }

    public long countOnlineUsers(OnlineUserFilter filter) {
        OnlineUserVisibleScope scope = onlineUserScopeHelper.resolveScope(filter);
        return loadVisibleOnlineUsers(scope, filter).size();
    }

    public SysUserOnline getOnlineUser(String tokenId) {
        if (StrUtil.isBlank(tokenId)) {
            return null;
        }
        SysUserOnline online = redisService.getCacheObject(JbmCacheConstants.ONLINE_TOKEN_KEY + tokenId);
        if (online == null) {
            return null;
        }
        enrichMissingFields(Collections.singletonList(online));
        return online;
    }

    public void assertTokenAccessible(String tokenId, OnlineUserFilter filter) {
        SysUserOnline online = getOnlineUser(tokenId);
        if (online == null) {
            throw new ServiceException("在线会话不存在或已失效");
        }
        OnlineUserVisibleScope scope = onlineUserScopeHelper.resolveScope(filter);
        onlineUserScopeHelper.assertAccessible(online, scope);
    }

    private List<SysUserOnline> loadVisibleOnlineUsers(OnlineUserVisibleScope scope, OnlineUserFilter filter) {
        List<SysUserOnline> all = loadAllFromRedis();
        enrichMissingFields(all);
        List<SysUserOnline> visible = all.stream()
                .filter(online -> onlineUserScopeHelper.isVisible(online, scope))
                .filter(online -> onlineUserScopeHelper.matchesTextFilter(online, filter))
                .collect(Collectors.toList());
        Collections.reverse(visible);
        return visible;
    }

    private List<SysUserOnline> loadAllFromRedis() {
        List<SysUserOnline> userOnlineList = new ArrayList<>();
        Collection<String> onlineKeys = redisService.keys(JbmCacheConstants.ONLINE_TOKEN_KEY + "*");
        if (onlineKeys == null || onlineKeys.isEmpty()) {
            return userOnlineList;
        }
        for (String onlineKey : onlineKeys) {
            try {
                SysUserOnline sysUserOnline = redisService.getCacheObject(onlineKey);
                if (sysUserOnline == null) {
                    continue;
                }
                String token = onlineKey.replace(JbmCacheConstants.ONLINE_TOKEN_KEY, "");
                if (StrUtil.isBlank(sysUserOnline.getTokenId())) {
                    sysUserOnline.setTokenId(token);
                }
                try {
                    StpUtil.getLoginIdByToken(token);
                } catch (Exception ex) {
                    redisService.deleteObject(onlineKey);
                    continue;
                }
                try {
                    Long activityTimeout = StpUtil.stpLogic.getTokenActivityTimeoutByToken(token);
                    if (activityTimeout != null && activityTimeout > 0) {
                        sysUserOnline.setActivityTime(DateUtil.offset(DateTime.now(), DateField.SECOND, activityTimeout.intValue()));
                    }
                    Long expireTime = getTokenExpire(token, onlineKey);
                    if (expireTime != null && expireTime > 0) {
                        sysUserOnline.setExpiredTime(DateUtil.offset(DateTime.now(), DateField.SECOND, expireTime.intValue()));
                    }
                } catch (Exception ignored) {
                }
                userOnlineList.add(sysUserOnline);
            } catch (Exception ignored) {
            }
        }
        userOnlineList.removeAll(Collections.singleton(null));
        return userOnlineList;
    }

    private Long getTokenExpire(String token, String onlineKey) {
        String tokenName = SaManager.getConfig().getTokenName();
        if (StrUtil.isBlank(tokenName)) {
            tokenName = "satoken";
        }
        Long tokenExpire = redisService.getExpire(tokenName + ":login:token:" + token);
        if (tokenExpire != null && tokenExpire > 0) {
            return tokenExpire;
        }
        return redisService.getExpire(onlineKey);
    }

    private void enrichMissingFields(List<SysUserOnline> onlineList) {
        if (onlineList == null || onlineList.isEmpty()) {
            return;
        }
        Set<Long> missingUserIds = new HashSet<>();
        Map<String, Long> tokenUserIds = new HashMap<>();
        for (SysUserOnline online : onlineList) {
            if (online == null || StrUtil.isBlank(online.getTokenId())) {
                continue;
            }
            if (online.getUserId() != null && online.getCompanyId() != null && online.getAppId() != null) {
                continue;
            }
            try {
                Object loginId = StpUtil.getLoginIdByToken(online.getTokenId());
                if (loginId == null) {
                    continue;
                }
                ParsedLoginId parsed = parseLoginId(String.valueOf(loginId));
                if (parsed.userId != null) {
                    tokenUserIds.put(online.getTokenId(), parsed.userId);
                    if (online.getUserId() == null) {
                        online.setUserId(parsed.userId);
                    }
                    if (online.getAppId() == null && parsed.appId != null) {
                        online.setAppId(parsed.appId);
                    }
                    if (online.getCompanyId() == null) {
                        missingUserIds.add(parsed.userId);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (missingUserIds.isEmpty()) {
            return;
        }
        Map<Long, BaseUser> userMap = baseUserService.getUsersByIds(new ArrayList<>(missingUserIds)).stream()
                .filter(u -> u.getUserId() != null)
                .collect(Collectors.toMap(BaseUser::getUserId, u -> u, (a, b) -> a));
        for (SysUserOnline online : onlineList) {
            if (online == null || online.getCompanyId() != null) {
                continue;
            }
            Long userId = online.getUserId();
            if (userId == null) {
                userId = tokenUserIds.get(online.getTokenId());
            }
            if (userId == null) {
                continue;
            }
            BaseUser user = userMap.get(userId);
            if (user == null) {
                continue;
            }
            if (online.getCompanyId() == null) {
                online.setCompanyId(user.getCompanyId());
            }
            if (online.getDeptId() == null) {
                online.setDeptId(user.getDepartmentId());
            }
        }
    }

    private ParsedLoginId parseLoginId(String loginId) {
        ParsedLoginId parsed = new ParsedLoginId();
        if (StrUtil.isBlank(loginId)) {
            return parsed;
        }
        String[] parts = loginId.split(":");
        if (parts.length >= 3) {
            try {
                parsed.appId = Long.parseLong(parts[1]);
            } catch (NumberFormatException ignored) {
            }
            try {
                parsed.userId = Long.parseLong(parts[2]);
            } catch (NumberFormatException ignored) {
            }
        }
        return parsed;
    }

    private static final class ParsedLoginId {
        private Long appId;
        private Long userId;
    }
}
