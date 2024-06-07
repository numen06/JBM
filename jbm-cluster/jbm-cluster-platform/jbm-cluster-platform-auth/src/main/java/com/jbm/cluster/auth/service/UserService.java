package com.jbm.cluster.auth.service;

import cn.dev33.satoken.exception.SaTokenException;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.ThirdPartyUserForm;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.api.service.IBaseUserServiceClient;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.util.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private IBaseUserServiceClient baseUserServiceClient;

    public JbmLoginUser findUserByUsername(String userName) {
        return baseUserServiceClient.userLogin(userName).action(new Function<UserAccount, JbmLoginUser>() {
            @Override
            public JbmLoginUser apply(UserAccount userAccount) {
                return userAccountToLoginUser(userAccount);
            }
        });
    }

    public JbmLoginUser loginAndRegisterMobileUser(String userName, String password) {
        ThirdPartyUserForm thirdPartyUserForm = new ThirdPartyUserForm();
        thirdPartyUserForm.setPassword(PasswordUtils.generatePassword(10));
        thirdPartyUserForm.setAccount(userName);
        thirdPartyUserForm.setPhone(userName);
        thirdPartyUserForm.setNickName(userName);
        ResultBody<UserAccount> resultBody = baseUserServiceClient.loginAndRegisterMobileUser(thirdPartyUserForm);
        if (!resultBody.getSuccess()) {
            throw new ServiceException(resultBody.getMessage());
        }
        return resultBody.action(userAccount -> {
            return userAccountToLoginUser(userAccount);
        });
    }


    public JbmLoginUser userAccountToLoginUser(UserAccount account) {
        if (ObjectUtil.isEmpty(account)) {
            throw new SaTokenException("用户授权不存在!");
        }
        JbmLoginUser jbmLoginUser = null;
        jbmLoginUser = new JbmLoginUser();
        jbmLoginUser.setUserId(account.getUserId());
        BaseUser baseUser = baseUserServiceClient.getUserInfoById(account.getUserId()).getResult();
        jbmLoginUser.setUsername(baseUser.getUserName());
        jbmLoginUser.setRealName(baseUser.getRealName());
        jbmLoginUser.setMobile(baseUser.getMobile());
        jbmLoginUser.setAccount(account.getAccount());
        jbmLoginUser.setAccountType(account.getAccountType());
        jbmLoginUser.setDeptId(account.getDepartmentId());
        jbmLoginUser.setCompanyId(account.getCompanyId());
        Set<String> roles = account.getRoles().stream().map(BaseRole::getRoleCode).collect(Collectors.toSet());
        jbmLoginUser.setRoles(roles);
        Set<Long> roleIds = account.getRoles().stream().map(BaseRole::getRoleId).collect(Collectors.toSet());
        jbmLoginUser.setRoleIds(roleIds);
        Set<String> menuPermission = account.getAuthorities().stream().map(OpenAuthority::getAuthority).collect(Collectors.toSet());
        jbmLoginUser.setMenuPermission(menuPermission);
        return jbmLoginUser;
    }

}
