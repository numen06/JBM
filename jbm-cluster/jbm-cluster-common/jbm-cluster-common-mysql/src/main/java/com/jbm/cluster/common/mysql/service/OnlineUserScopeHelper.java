package com.jbm.cluster.common.mysql.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.cluster.api.model.auth.SysUserOnline;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.exceptions.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 在线用户数据可见范围：复用 {@link OrgDataScopeHelper} 的组织规则。
 */
@Component
public class OnlineUserScopeHelper {

    @Autowired
    private OrgDataScopeHelper orgDataScopeHelper;

    public OnlineUserVisibleScope resolveScope(OnlineUserFilter filter) {
        OnlineUserVisibleScope scope = new OnlineUserVisibleScope();
        if (filter != null) {
            scope.setFilterAppId(filter.getAppId());
            scope.setFilterCompanyId(filter.getCompanyId());
        }
        if (LoginHelper.isAdmin()) {
            scope.setAdmin(true);
            return scope;
        }
        BaseUserForm userForm = new BaseUserForm();
        if (orgDataScopeHelper.applyUserQueryScope(userForm)) {
            scope.setSelfOnly(true);
            scope.setSelfUserId(LoginHelper.getUserId());
            return scope;
        }
        scope.setCompanyId(userForm.getCompanyId());
        scope.setCompanyIds(userForm.getCompanyIds());
        scope.setDepartmentIds(userForm.getDepartmentIds());
        return scope;
    }

    public boolean isVisible(SysUserOnline online, OnlineUserVisibleScope scope) {
        if (online == null || scope == null) {
            return false;
        }
        if (scope.getFilterAppId() != null && !ObjectUtil.equal(scope.getFilterAppId(), online.getAppId())) {
            return false;
        }
        if (scope.isAdmin()) {
            if (scope.getFilterCompanyId() != null && !ObjectUtil.equal(scope.getFilterCompanyId(), online.getCompanyId())) {
                return false;
            }
            return true;
        }
        if (scope.isSelfOnly()) {
            return scope.getSelfUserId() != null && ObjectUtil.equal(scope.getSelfUserId(), online.getUserId());
        }
        return matchesOrgScope(online, scope);
    }

    public void assertAccessible(SysUserOnline online, OnlineUserVisibleScope scope) {
        if (!isVisible(online, scope)) {
            throw new ServiceException("无权操作该在线会话");
        }
    }

    private boolean matchesOrgScope(SysUserOnline online, OnlineUserVisibleScope scope) {
        Long companyId = online.getCompanyId();
        if (companyId == null) {
            return false;
        }
        List<Long> companyIds = scope.getCompanyIds();
        if (companyIds != null && !companyIds.isEmpty()) {
            if (!companyIds.contains(companyId)) {
                return false;
            }
        } else if (scope.getCompanyId() != null && !ObjectUtil.equal(scope.getCompanyId(), companyId)) {
            return false;
        }
        List<Long> departmentIds = scope.getDepartmentIds();
        if (departmentIds != null && !departmentIds.isEmpty()) {
            Long deptId = online.getDeptId();
            if (deptId != null && !departmentIds.contains(deptId)) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesTextFilter(SysUserOnline online, OnlineUserFilter filter) {
        if (filter == null) {
            return true;
        }
        if (StrUtil.isNotEmpty(filter.getIpaddr()) && StrUtil.isNotEmpty(filter.getUserName())) {
            return StrUtil.equals(filter.getIpaddr(), online.getIpaddr())
                    && StrUtil.equals(filter.getUserName(), online.getUserName());
        }
        if (StrUtil.isNotEmpty(filter.getIpaddr())) {
            return StrUtil.equals(filter.getIpaddr(), online.getIpaddr());
        }
        if (StrUtil.isNotEmpty(filter.getUserName())) {
            return StrUtil.equals(filter.getUserName(), online.getUserName());
        }
        return true;
    }
}
