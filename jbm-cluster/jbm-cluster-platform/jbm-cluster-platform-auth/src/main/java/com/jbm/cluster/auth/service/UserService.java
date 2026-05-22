package com.jbm.cluster.auth.service;

import cn.dev33.satoken.exception.SaTokenException;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.ThirdPartyUserForm;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.auth.business.AuthUserBusiness;
import com.jbm.cluster.common.mysql.service.BaseUserService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.util.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private AuthUserBusiness authUserBusiness;
    @Autowired
    private BaseUserService baseUserService;

    public JbmLoginUser findUserByUsername(String userName) {
        UserAccount userAccount = authUserBusiness.login(userName);
        if (userAccount == null) {
            throw new ServiceException("用户不存在");
        }
        return userAccountToLoginUser(userAccount);
    }

    public JbmLoginUser loginAndRegisterMobileUser(String userName, String password) {
        ThirdPartyUserForm form = new ThirdPartyUserForm();
        form.setPassword(PasswordUtils.generatePassword(10));
        form.setAccount(userName);
        form.setPhone(userName);
        form.setNickName(userName);
        UserAccount userAccount = authUserBusiness.loginAndRegisterMobileUser(form);
        return userAccountToLoginUser(userAccount);
    }

    public JbmLoginUser userAccountToLoginUser(UserAccount account) {
        if (ObjectUtil.isEmpty(account)) {
            throw new SaTokenException("用户授权不存在!");
        }
        JbmLoginUser jbmLoginUser = new JbmLoginUser();
        jbmLoginUser.setUserId(account.getUserId());
        BaseUser baseUser = baseUserService.getUserById(account.getUserId());
        jbmLoginUser.setUsername(baseUser.getUserName());
        jbmLoginUser.setRealName(baseUser.getRealName());
        jbmLoginUser.setMobile(baseUser.getMobile());
        jbmLoginUser.setAccount(account.getAccount());
        jbmLoginUser.setAccountType(account.getAccountType());
        jbmLoginUser.setDeptId(account.getDepartmentId());
        jbmLoginUser.setCompanyId(account.getCompanyId());
        jbmLoginUser.setUserType(baseUser.getUserType());
        Set<String> roles = account.getRoles().stream().map(BaseRole::getRoleCode).collect(Collectors.toSet());
        jbmLoginUser.setRoles(roles);
        Set<Long> roleIds = account.getRoles().stream().map(BaseRole::getRoleId).collect(Collectors.toSet());
        jbmLoginUser.setRoleIds(roleIds);
        Set<String> menuPermission = account.getAuthorities().stream().map(OpenAuthority::getAuthority).collect(Collectors.toSet());
        jbmLoginUser.setMenuPermission(menuPermission);
        return jbmLoginUser;
    }
}
