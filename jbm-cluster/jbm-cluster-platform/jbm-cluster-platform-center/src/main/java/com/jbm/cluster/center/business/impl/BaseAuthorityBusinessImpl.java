package com.jbm.cluster.center.business.impl;

import com.jbm.cluster.api.form.BaseAuthorityRoleForm;
import com.jbm.cluster.api.form.BaseAuthorityUserForm;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.center.business.BaseAuthorityBusiness;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class BaseAuthorityBusinessImpl implements BaseAuthorityBusiness {

    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;

    private void refreshGateway() {
        jbmClusterTemplate.refreshGateway();
    }

    @Override
    public void grantAuthorityRole(Long roleId, Date expireTime, String[] authorityIds) {
        baseAuthorityService.addAuthorityRole(roleId, expireTime, authorityIds);
        refreshGateway();
    }

    @Override
    public void grantAuthorityRole(BaseAuthorityRoleForm form) {
        baseAuthorityService.addAuthorityRole(form.getRoleId(), form.getExpireTime(), form.getAuthorityIds());
        refreshGateway();
    }

    @Override
    public void grantAuthorityUser(Long userId, Date expireTime, String[] authorityIds) {
        baseAuthorityService.addAuthorityUser(userId, expireTime, authorityIds);
        refreshGateway();
    }

    @Override
    public void grantAuthorityUser(BaseAuthorityUserForm form) {
        baseAuthorityService.addAuthorityUser(form.getUserId(), form.getExpireTime(), form.getAuthorityIds());
        refreshGateway();
    }

    @Override
    public void grantAuthorityApp(Long appId, Date expireTime, String[] authorityIds) {
        baseAuthorityService.addAuthorityApp(appId, expireTime, authorityIds);
        refreshGateway();
    }

    @Override
    public void grantAuthorityAction(Long actionId, String[] authorityIds) {
        baseAuthorityService.addAuthorityAction(actionId, authorityIds);
        refreshGateway();
    }

    @Override
    public List<OpenAuthority> findAuthorityByUserId(Long userId, boolean rootUser) {
        return baseAuthorityService.findAuthorityByUser(userId, rootUser);
    }
}
