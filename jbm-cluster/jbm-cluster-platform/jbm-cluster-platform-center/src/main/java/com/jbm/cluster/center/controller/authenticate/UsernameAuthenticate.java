package com.jbm.cluster.center.controller.authenticate;

import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.cluster.center.service.BaseAccountService;
import com.jbm.cluster.center.service.BaseUserService;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
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

    @Autowired
    private BaseAccountService baseAccountService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public ResultBody<JbmLoginUser> login(String username, String password, String loginType) {
        UserAccount account = baseUserService.login(username, LoginType.PASSWORD.toString().equals(loginType) ? null : loginType.toLowerCase());
        if (account == null) {
            //小程序自动注册
            if (LoginType.MINIAPP.toString().equals(loginType)) {
                String key = password + "-PHONE";
                if (stringRedisTemplate.hasKey(key)) {
                    String phone = stringRedisTemplate.opsForValue().get(key);
                    account = baseUserService.registerAccountByPhone(phone, username, password, loginType.toLowerCase());
                }
            } else {
                return ResultBody.error("没有找到此用户");
            }
        }
        JbmLoginUser jbmLoginUser = null;
        if (LoginType.MINIAPP.toString().equals(loginType)) {
            jbmLoginUser = findUserByAccount(account);
            return ResultBody.ok().data(jbmLoginUser);
        }
        if (LoginType.WECHAT.toString().equals(loginType)) {
            jbmLoginUser = findUserByAccount(account);
            return ResultBody.ok().data(jbmLoginUser);
        }
        if (SecurityUtils.getPasswordEncoder().matches(password, account.getPassword())) {
            jbmLoginUser = findUserByAccount(account);
            return ResultBody.ok().data(jbmLoginUser);
        } else {
            return ResultBody.error("密码错误");
        }
    }

    public JbmLoginUser findUserByAccount(UserAccount account) {
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
        Set<String> menuPermission = account.getAuthorities().stream().map(OpenAuthority::getAuthority).collect(Collectors.toSet());
        jbmLoginUser.setMenuPermission(menuPermission);
        return jbmLoginUser;
    }

    @Override
    public List<LoginType> getLoginType() {
        return Lists.newArrayList(LoginType.PASSWORD, LoginType.MINIAPP, LoginType.WECHAT);
    }
}
