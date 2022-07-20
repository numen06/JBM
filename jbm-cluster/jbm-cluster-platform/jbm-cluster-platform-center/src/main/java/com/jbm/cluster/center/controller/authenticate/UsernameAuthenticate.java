package com.jbm.cluster.center.controller.authenticate;

import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.cluster.center.service.BaseUserService;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Created wesley.zhang
 * @Date 2022/5/19 16:36
 * @Description TODO
 */
@Service
public class UsernameAuthenticate implements ILoginAuthenticate {

    @Autowired
    private BaseUserService baseUserService;

    @Override
    public ResultBody<JbmLoginUser> login(String username, String password, String loginType) {
        UserAccount account = baseUserService.login(username);
        if (account == null) {
            return ResultBody.error("没有找到此用户");
        }
        JbmLoginUser jbmLoginUser = null;
        if (SecurityUtils.getPasswordEncoder().matches(password, account.getPassword())) {
            jbmLoginUser = new JbmLoginUser();
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
            Set<String> menuPermission = account.getAuthorities().stream().map(OpenAuthority::getAuthority).collect(Collectors.toSet());
            jbmLoginUser.setMenuPermission(menuPermission);
            return ResultBody.ok().data(jbmLoginUser);
        } else {
            return ResultBody.error("密码错误");
        }
    }

    @Override
    public LoginType getLoginType() {
        return LoginType.PASSWORD;
    }
}
