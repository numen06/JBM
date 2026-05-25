package com.jbm.cluster.center.business.impl;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.dashboard.DashboardIdentity;
import com.jbm.cluster.api.model.dashboard.DashboardMetric;
import com.jbm.cluster.api.model.dashboard.DashboardOverview;
import com.jbm.cluster.api.model.dashboard.DashboardPlatformCounts;
import com.jbm.cluster.api.model.dashboard.DashboardRisk;
import com.jbm.cluster.api.model.dashboard.DashboardSection;
import com.jbm.cluster.center.business.DashboardBusiness;
import com.jbm.cluster.common.mysql.service.BaseUserService;
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

    @Autowired
    private BaseUserService baseUserService;
    @Autowired
    private DashboardCacheService dashboardCacheService;

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
        identity.setVisibleMenuCount(
                dashboardCacheService.countVisibleMenus(loginUser.getUserId(), loginUser.getAppId(), platformAdmin));
        return identity;
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
        boolean needPlatformCounts = canViewUsersTotal(loginUser, platformAdmin)
                || canViewApps(loginUser, platformAdmin)
                || canViewOrgs(loginUser, platformAdmin)
                || canViewRoles(loginUser, platformAdmin)
                || canViewAuthorityCatalog(loginUser, platformAdmin)
                || canViewApiRegistry(loginUser, platformAdmin)
                || canViewApiKeys(loginUser, platformAdmin);

        DashboardPlatformCounts platformCounts = null;
        if (needPlatformCounts) {
            platformCounts = dashboardCacheService.loadPlatformCounts();
        }

        if (canViewUsersTotal(loginUser, platformAdmin) && platformCounts != null) {
            metrics.setUsersTotal(platformCounts.getUsersTotal());
        }
        if (canViewOnlineUsers(loginUser, platformAdmin)) {
            metrics.setOnlineUser(dashboardCacheService.countOnlineUsers(appId, platformAdmin));
        }
        if (canViewApps(loginUser, platformAdmin) && platformCounts != null) {
            metrics.setAppCount(platformCounts.getAppCount());
        }
        if (canViewOrgs(loginUser, platformAdmin) && platformCounts != null) {
            metrics.setOrgCount(platformCounts.getOrgCount());
        }
        if (canViewRoles(loginUser, platformAdmin) && platformCounts != null) {
            metrics.setRoleCount(platformCounts.getRoleCount());
        }
        if (canViewAuthorityCatalog(loginUser, platformAdmin) && platformCounts != null) {
            metrics.setAuthorityResourceCount(platformCounts.getAuthorityResourceCount());
        }
        if (canViewApiRegistry(loginUser, platformAdmin) && platformCounts != null) {
            metrics.setApiCount(platformCounts.getApiCount());
        }
        if (canViewApiKeys(loginUser, platformAdmin) && platformCounts != null) {
            metrics.setApiKeyCount(platformCounts.getApiKeyCount());
        }
        return metrics;
    }

    private List<DashboardRisk> buildRisks(JbmLoginUser loginUser, boolean platformAdmin, Long appId) {
        if (!canViewAuthorityCatalog(loginUser, platformAdmin)) {
            return Collections.emptyList();
        }
        return dashboardCacheService.loadAuthorityRisks(appId, platformAdmin);
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
