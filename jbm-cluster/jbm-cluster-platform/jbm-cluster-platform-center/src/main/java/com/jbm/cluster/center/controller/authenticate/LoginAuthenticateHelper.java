package com.jbm.cluster.center.controller.authenticate;

import com.jbm.cluster.api.constants.AccountType;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.center.service.BaseUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LoginAuthenticateHelper {

    @Autowired
    private BaseUserService baseUserService;

    public JbmLoginUser loginByAccount(String account, AccountType accountType) throws AccountException {
        UserAccount userAccount = baseUserService.login(account, accountType.toString());
        if (account == null) {
            throw new AccountException("没有找到该账号");
        }
        return userAccountToLoginUser(userAccount);
    }

    public JbmLoginUser userAccountToLoginUser(UserAccount account) {
        JbmLoginUser jbmLoginUser = new JbmLoginUser();
        jbmLoginUser.setUserId(account.getUserId());
        BaseUser baseUser = baseUserService.getUserById(account.getUserId());
        jbmLoginUser.setUsername(baseUser.getUserName());
        jbmLoginUser.setRealName(baseUser.getRealName());
        jbmLoginUser.setMobile(baseUser.getMobile());
        jbmLoginUser.setAccount(account.getAccount());
        jbmLoginUser.setAccountType(account.getAccountType());
        jbmLoginUser.setDeptId(account.getDepartmentId());
        Set<String> roles = account.getRoles().stream().map(BaseRole::getRoleCode).collect(Collectors.toSet());
        jbmLoginUser.setRoles(roles);
        Set<Long> roleIds = account.getRoles().stream().map(BaseRole::getRoleId).collect(Collectors.toSet());
        jbmLoginUser.setRoleIds(roleIds);
        Set<String> menuPermission = account.getAuthorities().stream().map(OpenAuthority::getAuthority).collect(Collectors.toSet());
        jbmLoginUser.setMenuPermission(menuPermission);
        return jbmLoginUser;
    }
}
