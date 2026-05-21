package com.jbm.cluster.center.controller.authenticate;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.AccountType;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.user.ThirdPartyUser;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.cluster.common.mysql.service.BaseAccountService;
import com.jbm.cluster.center.business.BaseUserBusiness;
import com.jbm.cluster.common.mysql.service.BaseUserService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
public class ThirdPartyAuthenticate implements ILoginAuthenticate {

    @Autowired
    private BaseUserBusiness baseUserBusiness;
    @Autowired
    private BaseUserService baseUserService;
    @Autowired
    private BaseAccountService baseAccountService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public ResultBody<JbmLoginUser> login(String username, String password, String loginType) {
        return ResultBody.callback(() -> {
            ThirdPartyUser thirdPartyUser = JSONObject.parseObject(username, ThirdPartyUser.class);
            if (thirdPartyUser == null) {
                throw ServiceException.of("三方授权信息错误");
            }
            log.info("[第三方认证]: 获取用户信息:{}", username);
            UserAccount userAccount = null;
            if (StrUtil.isEmpty(thirdPartyUser.getSubjectId())) {
                if (StrUtil.isNotEmpty(thirdPartyUser.getUsername())) {
                    thirdPartyUser.setSubjectId(thirdPartyUser.getUsername());
                } else if (StrUtil.isNotEmpty(thirdPartyUser.getMobile())) {
                    thirdPartyUser.setSubjectId(thirdPartyUser.getMobile());
                } else if (StrUtil.isNotEmpty(thirdPartyUser.getEmail())) {
                    thirdPartyUser.setSubjectId(thirdPartyUser.getEmail());
                }
                if (StrUtil.isEmpty(thirdPartyUser.getSubjectId())) {
                    throw ServiceException.of("没有找到第三方登录授权信息");
                }
            }
            userAccount = baseUserBusiness.login(thirdPartyUser.getSubjectId(), thirdPartyUser.getProvider());
            if (userAccount == null) {
                log.info("[第三方认证]: 没有用户信息:{}", thirdPartyUser.getSubjectId());
                throw ServiceException.of("没有找到授权");
            }
            JbmLoginUser jbmLoginUser = findUserByAccount(userAccount);
            jbmLoginUser.setThirdToken(thirdPartyUser.getToken());
            log.info("[第三方认证]: 获取用户信息成功: 用户名:{},菜单权限数量:{}", jbmLoginUser.getUsername(), CollUtil.size(jbmLoginUser.getMenuPermission()));
            return jbmLoginUser;
        });
    }

    public JbmLoginUser findUserByAccount(UserAccount account) {
        JbmLoginUser jbmLoginUser = new JbmLoginUser();
        jbmLoginUser.setUserId(account.getUserId());
        BaseUser baseUser = baseUserService.getUserById(account.getUserId());
        log.info("[第三方认证]: 获取用户信息:{}", baseUser);
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

    @Override
    public List<LoginType> getLoginType() {
        return Lists.newArrayList(LoginType.THIRD_PARTY);
    }
}
