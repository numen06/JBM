package com.jbm.cluster.center.business;

import com.jbm.cluster.api.form.BaseAuthorityRoleForm;
import com.jbm.cluster.api.form.BaseAuthorityUserForm;
import com.jbm.cluster.api.model.auth.OpenAuthority;

import java.util.Date;
import java.util.List;

public interface BaseAuthorityBusiness {

    void grantAuthorityRole(Long roleId, Date expireTime, String[] authorityIds);

    void grantAuthorityRole(BaseAuthorityRoleForm form);

    void grantAuthorityUser(Long userId, Date expireTime, String[] authorityIds);

    void grantAuthorityUser(BaseAuthorityUserForm form);

    void grantAuthorityApp(Long appId, Date expireTime, String[] authorityIds);

    void grantAuthorityAction(Long actionId, String[] authorityIds);

    List<OpenAuthority> findAuthorityByUserId(Long userId, boolean rootUser);
}
