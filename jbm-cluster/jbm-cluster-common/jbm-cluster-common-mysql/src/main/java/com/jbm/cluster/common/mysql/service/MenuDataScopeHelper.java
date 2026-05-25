package com.jbm.cluster.common.mysql.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.entitys.basic.BaseMenu;
import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.exceptions.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单管理数据范围：平台超管可管理全部；租户管理员仅可管理本组织应用菜单 + 只读平台菜单。
 */
@Component
public class MenuDataScopeHelper {

    @Autowired
    private BaseAppService baseAppService;
    @Autowired
    private BaseUserOrgService baseUserOrgService;
    @Autowired
    private BaseOrgService baseOrgService;

    public boolean isPlatformAdmin() {
        return LoginHelper.isAdmin();
    }

    public Set<Long> resolveManageableOrgIds() {
        if (isPlatformAdmin()) {
            return null;
        }
        Long userId = LoginHelper.getUserId();
        Long companyId = LoginHelper.getCompanyId();
        Long deptId = LoginHelper.getDeptId();
        if (companyId == null && deptId != null) {
            BaseOrg dept = baseOrgService.selectById(deptId);
            if (dept != null) {
                BaseOrg top = baseOrgService.findTopCompany(dept);
                if (top != null) {
                    companyId = top.getId();
                }
            }
        }
        Set<Long> orgIds = new LinkedHashSet<>();
        if (companyId != null) {
            orgIds.add(companyId);
        }
        if (userId != null) {
            orgIds.addAll(baseUserOrgService.getActiveOrgIds(userId));
        }
        return orgIds;
    }

    /** 可管理的应用 ID；平台超管返回 null 表示不限制。 */
    public List<Long> resolveManageableAppIds() {
        if (isPlatformAdmin()) {
            return null;
        }
        Set<Long> orgIds = resolveManageableOrgIds();
        if (orgIds == null || orgIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<BaseApp> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(BaseApp::getOrgId, orgIds);
        List<BaseApp> apps = baseAppService.selectEntitys(queryWrapper);
        if (apps == null || apps.isEmpty()) {
            return Collections.emptyList();
        }
        return apps.stream()
                .map(BaseApp::getAppId)
                .filter(ObjectUtil::isNotEmpty)
                .collect(Collectors.toList());
    }

    public void applyToMenuQuery(LambdaQueryWrapper<BaseMenu> lambda) {
        if (isPlatformAdmin()) {
            return;
        }
        List<Long> appIds = resolveManageableAppIds();
        lambda.and(w -> {
            w.isNull(BaseMenu::getAppId);
            if (appIds != null && !appIds.isEmpty()) {
                w.or().in(BaseMenu::getAppId, appIds);
            }
        });
    }

    public void assertCanManageMenu(BaseMenu menu) {
        if (menu == null) {
            throw new ServiceException("菜单不存在");
        }
        assertCanManageAppId(menu.getAppId());
    }

    public void assertCanManageAppId(Long appId) {
        if (ObjectUtil.isEmpty(appId)) {
            if (!isPlatformAdmin()) {
                throw new ServiceException("平台公共菜单仅平台超管可管理");
            }
            return;
        }
        if (isPlatformAdmin()) {
            return;
        }
        List<Long> appIds = resolveManageableAppIds();
        if (appIds == null || !appIds.contains(appId)) {
            throw new ServiceException("无权管理该应用下的菜单");
        }
    }

    public void assertCanModifyExistingMenu(BaseMenu existing, BaseMenu incoming) {
        if (existing == null) {
            throw new ServiceException("菜单不存在");
        }
        assertCanManageMenu(existing);
        assertCanManageAppId(incoming.getAppId());
        if (!isPlatformAdmin() && incoming.getAppId() == null) {
            throw new ServiceException("仅平台超管可将菜单设为平台公共菜单");
        }
    }
}
