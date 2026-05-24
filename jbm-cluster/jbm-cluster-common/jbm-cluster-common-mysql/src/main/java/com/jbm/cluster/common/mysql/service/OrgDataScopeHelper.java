package com.jbm.cluster.common.mysql.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 解析当前登录用户的数据可见范围：主组织 + 本部门及下级 + 跨组织授权。
 */
@Component
public class OrgDataScopeHelper {

    @Autowired
    private BaseOrgService baseOrgService;
    @Autowired
    private BaseUserOrgService baseUserOrgService;

    /**
     * 将数据范围写入查询表单；返回 true 表示仅查本人。
     */
    public boolean applyUserQueryScope(BaseUserForm form) {
        if (LoginHelper.isAdmin()) {
            return false;
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

        Set<Long> companyIds = new LinkedHashSet<>();
        if (companyId != null) {
            companyIds.add(companyId);
        }
        if (userId != null) {
            companyIds.addAll(baseUserOrgService.getActiveOrgIds(userId));
        }
        if (companyIds.isEmpty()) {
            form.setUserId(userId);
            return true;
        }
        if (companyIds.size() == 1) {
            Long only = companyIds.iterator().next();
            form.setCompanyId(only);
            if (deptId != null && ObjectUtil.equal(only, companyId)) {
                applyDepartmentSubtree(form, deptId);
            }
            return false;
        }
        form.setCompanyIds(new ArrayList<>(companyIds));
        return false;
    }

    private void applyDepartmentSubtree(BaseUserForm form, Long deptId) {
        BaseOrg dept = baseOrgService.selectById(deptId);
        if (dept == null) {
            return;
        }
        List<BaseOrg> subtree = baseOrgService.findRelegationCompany(dept);
        List<Long> deptIds = subtree.stream()
                .map(BaseOrg::getId)
                .filter(ObjectUtil::isNotEmpty)
                .collect(Collectors.toList());
        if (!deptIds.isEmpty()) {
            form.setDepartmentIds(deptIds);
        }
    }

    /** 将范围应用到 MyBatis-Plus 条件（如关键字检索）。 */
    public void applyToUserQuery(QueryWrapper<BaseUser> queryWrapper) {
        if (LoginHelper.isAdmin()) {
            return;
        }
        BaseUserForm scope = new BaseUserForm();
        if (applyUserQueryScope(scope)) {
            if (scope.getUserId() != null) {
                queryWrapper.lambda().eq(BaseUser::getUserId, scope.getUserId());
            }
            return;
        }
        if (scope.getCompanyIds() != null && !scope.getCompanyIds().isEmpty()) {
            queryWrapper.lambda().in(BaseUser::getCompanyId, scope.getCompanyIds());
            return;
        }
        if (scope.getCompanyId() != null) {
            queryWrapper.lambda().eq(BaseUser::getCompanyId, scope.getCompanyId());
        }
        if (scope.getDepartmentIds() != null && !scope.getDepartmentIds().isEmpty()) {
            queryWrapper.lambda().in(BaseUser::getDepartmentId, scope.getDepartmentIds());
        }
    }
}
