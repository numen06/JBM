package com.jbm.cluster.center.business.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.constants.ResourceType;
import com.jbm.cluster.api.entitys.auth.AuthorityMenu;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.api.entitys.basic.BaseApiKey;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.entitys.basic.BaseAuthority;
import com.jbm.cluster.api.entitys.basic.BaseMenu;
import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.model.dashboard.DashboardPlatformCounts;
import com.jbm.cluster.api.model.dashboard.DashboardRisk;
import com.jbm.cluster.common.mysql.mapper.BaseActionMapper;
import com.jbm.cluster.common.mysql.mapper.BaseApiMapper;
import com.jbm.cluster.common.mysql.mapper.BaseAuthorityMapper;
import com.jbm.cluster.common.mysql.mapper.BaseMenuMapper;
import com.jbm.cluster.common.mysql.service.BaseApiKeyService;
import com.jbm.cluster.common.mysql.service.BaseAppService;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.common.mysql.service.BaseOrgService;
import com.jbm.cluster.common.mysql.service.BaseRoleService;
import com.jbm.cluster.common.mysql.service.BaseUserService;
import com.jbm.cluster.common.mysql.service.OnlineUserFilter;
import com.jbm.cluster.common.mysql.service.OnlineUserMonitorService;
import com.jbm.cluster.core.constant.JbmCacheConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 仪表盘统计缓存层（独立 Bean 以生效 Spring Cache AOP）。
 * TTL 通过缓存名 {@code namespace#秒} 配置，见 {@link jbm.framework.boot.autoconfigure.redis.cache.CustomizedRedisCacheManager}。
 */
@Service
public class DashboardCacheService {

    private static final int RISK_SCAN_LIMIT = 200;

    @Autowired
    private BaseUserService baseUserService;
    @Autowired
    private BaseAppService baseAppService;
    @Autowired
    private BaseOrgService baseOrgService;
    @Autowired
    private BaseRoleService baseRoleService;
    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private BaseApiKeyService baseApiKeyService;
    @Autowired
    private OnlineUserMonitorService onlineUserMonitorService;
    @Autowired
    private BaseMenuMapper baseMenuMapper;
    @Autowired
    private BaseActionMapper baseActionMapper;
    @Autowired
    private BaseApiMapper baseApiMapper;
    @Autowired
    private BaseAuthorityMapper baseAuthorityMapper;

    @Cacheable(
            cacheNames = JbmCacheConstants.DASHBOARD_PLATFORM_COUNTS + "#120",
            key = "'snapshot'",
            unless = "#result == null")
    public DashboardPlatformCounts loadPlatformCounts() {
        DashboardPlatformCounts counts = new DashboardPlatformCounts();
        counts.setUsersTotal(baseUserService.count(new BaseUser()));
        counts.setAppCount(baseAppService.count(new BaseApp()));
        counts.setOrgCount(baseOrgService.count(new BaseOrg()));
        counts.setRoleCount(baseRoleService.count(new BaseRole()));
        counts.setAuthorityResourceCount(baseAuthorityService.count(new BaseAuthority()));
        counts.setApiCount(baseApiMapper.selectCount(null));
        counts.setApiKeyCount(baseApiKeyService.count(new BaseApiKey()));
        return counts;
    }

    @Cacheable(
            cacheNames = JbmCacheConstants.DASHBOARD_ONLINE_USERS + "#25",
            key = "#platformAdmin ? 'all' : ('app:' + (#appId != null ? #appId : 0))")
    public long countOnlineUsers(Long appId, boolean platformAdmin) {
        OnlineUserFilter filter = new OnlineUserFilter();
        if (!platformAdmin && appId != null) {
            filter.setAppId(appId);
        }
        return onlineUserMonitorService.countOnlineUsers(filter);
    }

    @Cacheable(
            cacheNames = JbmCacheConstants.DASHBOARD_VISIBLE_MENUS + "#60",
            key = "#userId + ':' + (#appId != null ? #appId : 0) + ':' + #platformAdmin")
    public int countVisibleMenus(Long userId, Long appId, boolean platformAdmin) {
        List<AuthorityMenu> menus = baseAuthorityService.findAuthorityMenuByUser(userId, appId, platformAdmin);
        return menus == null ? 0 : menus.size();
    }

    @Cacheable(
            cacheNames = JbmCacheConstants.DASHBOARD_RISKS + "#300",
            key = "#platformAdmin ? 'platform' : ('app:' + (#appId != null ? #appId : 0))")
    public List<DashboardRisk> loadAuthorityRisks(Long appId, boolean platformAdmin) {
        List<DashboardRisk> risks = new ArrayList<>();
        appendMenuWithoutAuthorityRisks(risks, appId, platformAdmin);
        appendOrphanAuthorityRisks(risks);
        appendUngrantedAuthorityRisks(risks);
        return risks.isEmpty() ? Collections.emptyList() : risks;
    }

    private void appendMenuWithoutAuthorityRisks(List<DashboardRisk> risks, Long appId, boolean platformAdmin) {
        QueryWrapper<BaseMenu> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseMenu::getStatus, 1);
        applyMenuAppScope(queryWrapper, appId, platformAdmin);
        queryWrapper.last("LIMIT " + RISK_SCAN_LIMIT);
        List<BaseMenu> menus = baseMenuMapper.selectList(queryWrapper);
        if (menus == null || menus.isEmpty()) {
            return;
        }
        long missing = menus.stream()
                .filter(menu -> baseAuthorityService.getAuthority(menu.getMenuId(), ResourceType.menu) == null)
                .count();
        if (missing > 0) {
            DashboardRisk risk = new DashboardRisk();
            risk.setLevel("warning");
            risk.setCode("menu_without_authority");
            risk.setTitle("存在 " + missing + " 个菜单尚未生成权限点");
            risk.setTarget("/system/menus");
            risks.add(risk);
        }
    }

    private void appendOrphanAuthorityRisks(List<DashboardRisk> risks) {
        QueryWrapper<BaseAuthority> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseAuthority::getStatus, 1);
        queryWrapper.last("LIMIT " + RISK_SCAN_LIMIT);
        List<BaseAuthority> authorities = baseAuthorityMapper.selectList(queryWrapper);
        if (authorities == null || authorities.isEmpty()) {
            return;
        }
        long orphanCount = 0;
        for (BaseAuthority authority : authorities) {
            if (isOrphanAuthority(authority)) {
                orphanCount++;
            }
        }
        if (orphanCount > 0) {
            DashboardRisk risk = new DashboardRisk();
            risk.setLevel("warning");
            risk.setCode("orphan_authority");
            risk.setTitle("存在 " + orphanCount + " 条权限点关联的资源已不存在");
            risk.setTarget("/authority/catalog");
            risks.add(risk);
        }
    }

    private void appendUngrantedAuthorityRisks(List<DashboardRisk> risks) {
        QueryWrapper<BaseAuthority> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseAuthority::getStatus, 1);
        queryWrapper.last("LIMIT " + RISK_SCAN_LIMIT);
        List<BaseAuthority> authorities = baseAuthorityMapper.selectList(queryWrapper);
        if (authorities == null || authorities.isEmpty()) {
            return;
        }
        long ungranted = 0;
        for (BaseAuthority authority : authorities) {
            ResourceType resourceType = resolveResourceType(authority);
            Long resourceId = resolveResourceId(authority);
            if (resourceType == null || resourceId == null) {
                continue;
            }
            if (!Boolean.TRUE.equals(baseAuthorityService.isGranted(resourceId, resourceType))) {
                ungranted++;
            }
        }
        if (ungranted > 0) {
            DashboardRisk risk = new DashboardRisk();
            risk.setLevel("info");
            risk.setCode("ungranted_authority");
            risk.setTitle("存在 " + ungranted + " 条权限点尚未授权给任何角色/用户/应用");
            risk.setTarget("/authority/roles");
            risks.add(risk);
        }
    }

    private boolean isOrphanAuthority(BaseAuthority authority) {
        if (authority.getMenuId() != null && baseMenuMapper.selectById(authority.getMenuId()) == null) {
            return true;
        }
        if (authority.getActionId() != null && baseActionMapper.selectById(authority.getActionId()) == null) {
            return true;
        }
        if (authority.getApiId() != null) {
            BaseApi api = baseApiMapper.selectById(authority.getApiId());
            return api == null;
        }
        return false;
    }

    private ResourceType resolveResourceType(BaseAuthority authority) {
        if (authority.getMenuId() != null) {
            return ResourceType.menu;
        }
        if (authority.getActionId() != null) {
            return ResourceType.action;
        }
        if (authority.getApiId() != null) {
            return ResourceType.api;
        }
        return null;
    }

    private Long resolveResourceId(BaseAuthority authority) {
        if (authority.getMenuId() != null) {
            return authority.getMenuId();
        }
        if (authority.getActionId() != null) {
            return authority.getActionId();
        }
        if (authority.getApiId() != null) {
            return authority.getApiId();
        }
        return null;
    }

    private void applyMenuAppScope(QueryWrapper<BaseMenu> queryWrapper, Long appId, boolean platformAdmin) {
        if (platformAdmin) {
            return;
        }
        if (appId == null) {
            return;
        }
        queryWrapper.lambda().and(w -> w.isNull(BaseMenu::getAppId).or().eq(BaseMenu::getAppId, appId));
    }
}
