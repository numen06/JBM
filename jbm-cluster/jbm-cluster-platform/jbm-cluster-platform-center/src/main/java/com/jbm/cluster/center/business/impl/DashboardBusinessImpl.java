package com.jbm.cluster.center.business.impl;

import cn.hutool.core.util.StrUtil;
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
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.dashboard.DashboardIdentity;
import com.jbm.cluster.api.model.dashboard.DashboardMetric;
import com.jbm.cluster.api.model.dashboard.DashboardOverview;
import com.jbm.cluster.api.model.dashboard.DashboardRisk;
import com.jbm.cluster.api.model.dashboard.DashboardSection;
import com.jbm.cluster.center.business.DashboardBusiness;
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
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class DashboardBusinessImpl implements DashboardBusiness {

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

    @Override
    public DashboardOverview buildOverview() {
        JbmLoginUser loginUser = LoginHelper.getLoginUser();
        boolean platformAdmin = LoginHelper.isAdmin();
        BaseUser baseUser = baseUserService.getUserById(loginUser.getUserId());
        if (baseUser != null && !platformAdmin) {
            platformAdmin = JbmConstants.isSuperUser(
                    baseUser.getUserId(), baseUser.getUserName(), baseUser.getUserType());
        }

        DashboardOverview overview = new DashboardOverview();
        overview.setIdentity(buildIdentity(loginUser, baseUser, platformAdmin));
        overview.setSections(buildSections(loginUser, platformAdmin));
        overview.setMetrics(buildMetrics(loginUser, platformAdmin, loginUser.getAppId()));
        overview.setRisks(buildRisks(loginUser, platformAdmin, loginUser.getAppId()));
        return overview;
    }

    private DashboardIdentity buildIdentity(JbmLoginUser loginUser, BaseUser baseUser, boolean platformAdmin) {
        DashboardIdentity identity = new DashboardIdentity();
        identity.setUserId(loginUser.getUserId());
        identity.setUserName(StrUtil.blankToDefault(loginUser.getUsername(), loginUser.getAccount()));
        if (baseUser != null) {
            identity.setNickName(baseUser.getNickName());
        }
        identity.setAppId(loginUser.getAppId());
        identity.setClientId(loginUser.getClientId());
        identity.setScope(platformAdmin ? "platform" : "app");
        if (loginUser.getRoles() != null) {
            identity.setRoles(new ArrayList<>(loginUser.getRoles()));
        } else {
            identity.setRoles(Collections.emptyList());
        }
        identity.setVisibleMenuCount(countVisibleMenus(loginUser, platformAdmin));
        return identity;
    }

    private int countVisibleMenus(JbmLoginUser loginUser, boolean platformAdmin) {
        List<AuthorityMenu> menus = baseAuthorityService.findAuthorityMenuByUser(
                loginUser.getUserId(), loginUser.getAppId(), platformAdmin);
        return menus == null ? 0 : menus.size();
    }

    private DashboardSection buildSections(JbmLoginUser loginUser, boolean platformAdmin) {
        DashboardSection sections = new DashboardSection();
        sections.setSystem(hasAnyMenu(loginUser, platformAdmin,
                "users", "onlineUsers", "orgs", "apps", "dicts", "extend_fields"));
        sections.setAuthority(hasAnyMenu(loginUser, platformAdmin,
                "roles", "authority", "menus", "actions"));
        sections.setApi(hasAnyMenu(loginUser, platformAdmin, "api_registry", "api_mgmt", "api_monitor"));
        sections.setGateway(hasAnyMenu(loginUser, platformAdmin,
                "gw_services", "gw_routes", "gw_rate", "gw_ip", "gw_gray"));
        sections.setDeveloper(hasAnyMenu(loginUser, platformAdmin,
                "developer_mgmt", "developer", "api_key_mgmt", "api_keys"));
        sections.setAudit(hasAnyMenu(loginUser, platformAdmin, "account_logs", "onlineUsers"));
        return sections;
    }

    private DashboardMetric buildMetrics(JbmLoginUser loginUser, boolean platformAdmin, Long appId) {
        DashboardMetric metrics = new DashboardMetric();
        if (canViewUsersTotal(loginUser, platformAdmin)) {
            metrics.setUsersTotal(baseUserService.count(new BaseUser()));
        }
        if (canViewOnlineUsers(loginUser, platformAdmin)) {
            OnlineUserFilter filter = new OnlineUserFilter();
            if (!platformAdmin && appId != null) {
                filter.setAppId(appId);
            }
            metrics.setOnlineUser(onlineUserMonitorService.countOnlineUsers(filter));
        }
        if (canViewApps(loginUser, platformAdmin)) {
            metrics.setAppCount(baseAppService.count(new BaseApp()));
        }
        if (canViewOrgs(loginUser, platformAdmin)) {
            metrics.setOrgCount(baseOrgService.count(new BaseOrg()));
        }
        if (canViewRoles(loginUser, platformAdmin)) {
            metrics.setRoleCount(baseRoleService.count(new BaseRole()));
        }
        if (canViewAuthorityCatalog(loginUser, platformAdmin)) {
            metrics.setAuthorityResourceCount(baseAuthorityService.count(new BaseAuthority()));
        }
        if (canViewApiRegistry(loginUser, platformAdmin)) {
            metrics.setApiCount(baseApiMapper.selectCount(null));
        }
        if (canViewApiKeys(loginUser, platformAdmin)) {
            metrics.setApiKeyCount(baseApiKeyService.count(new BaseApiKey()));
        }
        return metrics;
    }

    private List<DashboardRisk> buildRisks(JbmLoginUser loginUser, boolean platformAdmin, Long appId) {
        if (!canViewAuthorityCatalog(loginUser, platformAdmin)) {
            return Collections.emptyList();
        }
        List<DashboardRisk> risks = new ArrayList<>();
        appendMenuWithoutAuthorityRisks(risks, appId, platformAdmin);
        appendOrphanAuthorityRisks(risks);
        appendUngrantedAuthorityRisks(risks);
        return risks;
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

    private boolean hasAnyMenu(JbmLoginUser loginUser, boolean platformAdmin, String... menuCodes) {
        if (platformAdmin) {
            return true;
        }
        Set<String> authorities = loginUser.getAuthorities();
        if (authorities == null || authorities.isEmpty()) {
            return false;
        }
        for (String code : menuCodes) {
            if (authorities.contains(JbmSecurityConstants.AUTHORITY_PREFIX_MENU + code)) {
                return true;
            }
        }
        return false;
    }

    private boolean canViewUsersTotal(JbmLoginUser loginUser, boolean platformAdmin) {
        return platformAdmin || hasAnyMenu(loginUser, false, "users", "user");
    }

    private boolean canViewOnlineUsers(JbmLoginUser loginUser, boolean platformAdmin) {
        return platformAdmin || hasAnyMenu(loginUser, false, "onlineUsers", "account_logs");
    }

    private boolean canViewApps(JbmLoginUser loginUser, boolean platformAdmin) {
        return platformAdmin || hasAnyMenu(loginUser, false, "apps", "app");
    }

    private boolean canViewOrgs(JbmLoginUser loginUser, boolean platformAdmin) {
        return platformAdmin || hasAnyMenu(loginUser, false, "orgs", "org");
    }

    private boolean canViewRoles(JbmLoginUser loginUser, boolean platformAdmin) {
        return platformAdmin || hasAnyMenu(loginUser, false, "roles", "role");
    }

    private boolean canViewAuthorityCatalog(JbmLoginUser loginUser, boolean platformAdmin) {
        return platformAdmin || hasAnyMenu(loginUser, false, "authority", "menus", "actions");
    }

    private boolean canViewApiRegistry(JbmLoginUser loginUser, boolean platformAdmin) {
        return platformAdmin || hasAnyMenu(loginUser, false, "api_registry", "api_mgmt", "authority");
    }

    private boolean canViewApiKeys(JbmLoginUser loginUser, boolean platformAdmin) {
        return platformAdmin || hasAnyMenu(loginUser, false, "api_key_mgmt", "api_keys", "developer_mgmt", "developer");
    }
}
