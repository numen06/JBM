package com.jbm.cluster.center.business.impl;

import com.jbm.cluster.api.form.BaseAuthorityRoleForm;
import com.jbm.cluster.api.form.BaseAuthorityUserForm;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.center.business.BaseAuthorityBusiness;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.mysql.service.impl.BaseAuthorityServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class BaseAuthorityBusinessImpl extends BaseAuthorityServiceImpl implements BaseAuthorityBusiness {

    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;

    private void refreshGateway() {
        jbmClusterTemplate.refreshGateway();
    }

    @Override
    public void grantAuthorityRole(Long roleId, Date expireTime, String[] authorityIds) {
        addAuthorityRole(roleId, expireTime, authorityIds);
        refreshGateway();
    }

    @Override
    public void grantAuthorityRole(BaseAuthorityRoleForm form) {
        addAuthorityRole(form.getRoleId(), form.getExpireTime(), form.getAuthorityIds());
        refreshGateway();
    }

    @Override
    public void grantAuthorityUser(Long userId, Date expireTime, String[] authorityIds) {
        addAuthorityUser(userId, expireTime, authorityIds);
        refreshGateway();
    }

    @Override
    public void grantAuthorityUser(BaseAuthorityUserForm form) {
        addAuthorityUser(form.getUserId(), form.getExpireTime(), form.getAuthorityIds());
        refreshGateway();
    }

    @Override
    public void grantAuthorityApp(Long appId, Date expireTime, String[] authorityIds) {
        addAuthorityApp(appId, expireTime, authorityIds);
        refreshGateway();
    }

    @Override
    public void grantAuthorityAction(Long actionId, String[] authorityIds) {
        addAuthorityAction(actionId, authorityIds);
        refreshGateway();
    }

    @Override
    public List<OpenAuthority> findAuthorityByUserId(Long userId, boolean rootUser) {
        return findAuthorityByUser(userId, rootUser);
    }
}