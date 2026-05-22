package com.jbm.cluster.auth.business;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.AccountType;
import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.ThirdPartyUserForm;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.common.mysql.service.BaseAccountService;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.common.mysql.service.BaseRoleService;
import com.jbm.cluster.common.mysql.service.BaseUserService;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.util.PasswordUtils;
import com.jbm.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 认证侧用户业务（从 center BaseUserBusinessImpl 抽取登录相关能力）
 */
@Slf4j
@Service
public class AuthUserBusiness {

    @Autowired
    private BaseUserService baseUserService;
    @Autowired
    private BaseRoleService roleService;
    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private BaseAccountService baseAccountService;

    public UserAccount login(String account) {
        return doLogin(account, null);
    }

    public UserAccount login(String account, String loginType) {
        return doLogin(account, loginType);
    }

    public UserAccount getUserAccount(Long userId) {
        List<OpenAuthority> authorities = Lists.newArrayList();
        List<BaseRole> rolesList = roleService.getUserRoles(userId);
        if (rolesList != null) {
            for (BaseRole role : rolesList) {
                OpenAuthority authority = new OpenAuthority(role.getRoleId().toString(),
                        JbmSecurityConstants.AUTHORITY_PREFIX_ROLE + role.getRoleCode(), null, "role");
                authorities.add(authority);
            }
        }
        BaseUser baseUser = baseUserService.getUserById(userId);
        if (baseUser == null) {
            return null;
        }
        if (Integer.valueOf(JbmConstants.ACCOUNT_STATUS_DISABLE).equals(baseUser.getStatus())) {
            if (ObjectUtil.isNotEmpty(baseUser.getCloseTime()) && baseUser.getCloseTime().before(DateUtil.endOfDay(DateTime.now()))) {
                throw new ServiceException("没有找到此用户");
            }
            throw new ServiceException("用户已停用，请联系管理员");
        }
        List<OpenAuthority> userGrantedAuthority = baseAuthorityService.findAuthorityByUser(userId,
                JbmConstants.ROOT.equals(baseUser.getUserName()));
        if (userGrantedAuthority != null && !userGrantedAuthority.isEmpty()) {
            authorities.addAll(userGrantedAuthority);
        }
        UserAccount userAccount = new UserAccount();
        BeanUtil.copyProperties(baseUser, userAccount);
        userAccount.setAuthorities(authorities);
        userAccount.setRoles(rolesList);
        return userAccount;
    }

    private UserAccount doLogin(String account, String loginType) {
        if (StringUtils.isBlank(account)) {
            return null;
        }
        BaseAccount baseAccount;
        if (StringUtils.isNotBlank(loginType)) {
            baseAccount = baseAccountService.getAccount(account, loginType, JbmConstants.ACCOUNT_DOMAIN_ADMIN);
        } else {
            baseAccount = baseAccountService.getAccount(account, JbmConstants.ACCOUNT_TYPE_USERNAME, JbmConstants.ACCOUNT_DOMAIN_ADMIN);
            if (ObjectUtil.isEmpty(baseAccount) && Validator.isMobile(account)) {
                baseAccount = baseAccountService.getAccount(account, JbmConstants.ACCOUNT_TYPE_MOBILE, JbmConstants.ACCOUNT_DOMAIN_ADMIN);
            }
            if (ObjectUtil.isEmpty(baseAccount) && Validator.isEmail(account)) {
                baseAccount = baseAccountService.getAccount(account, JbmConstants.ACCOUNT_TYPE_EMAIL, JbmConstants.ACCOUNT_DOMAIN_ADMIN);
            }
        }
        if (baseAccount == null) {
            return null;
        }
        BaseUser baseUser = baseUserService.getUserById(baseAccount.getUserId());
        if (ObjectUtil.isEmpty(baseUser) || (ObjectUtil.isNotEmpty(baseUser.getCloseTime())
                && baseUser.getCloseTime().before(DateUtil.endOfDay(DateTime.now())))) {
            return null;
        }
        UserAccount userAccount = getUserAccount(baseAccount.getUserId());
        BeanUtils.copyProperties(baseAccount, userAccount);
        userAccount.setAccountType(baseAccount.getAccountType());
        return userAccount;
    }

    public void register(BaseUser baseUser, String registerIp) {
        validationExist(baseUser);
        PasswordUtils.checkPassword(baseUser.getPassword());
        baseUser.setStatus(JbmConstants.ACCOUNT_STATUS_NORMAL);
        baseUser.setUserType(JbmConstants.USER_TYPE_NORMAL);
        baseUserService.insertEntity(baseUser);
        baseAccountService.register(baseUser.getUserId(), baseUser.getUserName(), baseUser.getPassword(),
                JbmConstants.ACCOUNT_TYPE_USERNAME, baseUser.getStatus(), JbmConstants.ACCOUNT_DOMAIN_ADMIN, registerIp);
        if (Validator.isEmail(baseUser.getEmail())) {
            baseAccountService.register(baseUser.getUserId(), baseUser.getEmail(), baseUser.getPassword(),
                    JbmConstants.ACCOUNT_TYPE_EMAIL, baseUser.getStatus(), JbmConstants.ACCOUNT_DOMAIN_ADMIN, registerIp);
        }
        if (Validator.isMobile(baseUser.getMobile())) {
            baseAccountService.register(baseUser.getUserId(), baseUser.getMobile(), baseUser.getPassword(),
                    JbmConstants.ACCOUNT_TYPE_MOBILE, baseUser.getStatus(), JbmConstants.ACCOUNT_DOMAIN_ADMIN, registerIp);
        }
    }

    public UserAccount registerAccountByPhone(String phone, String username, String password, String accountType) {
        ThirdPartyUserForm form = new ThirdPartyUserForm();
        form.setPhone(phone);
        form.setAccountType(accountType);
        form.setAccount(username);
        form.setPassword(password);
        return loginAndRegisterMobileUser(form);
    }

    public UserAccount loginAndRegisterMobileUser(ThirdPartyUserForm form) {
        UserAccount userAccount = doLogin(form.getAccount(), "mobile");
        if (ObjectUtil.isNotEmpty(userAccount)) {
            return userAccount;
        }
        if (StrUtil.isBlank(form.getPhone())) {
            throw new ServiceException("手机为空");
        }
        BaseUser user = baseUserService.getUserByPhone(form.getPhone());
        if (ObjectUtil.isEmpty(user) || (ObjectUtil.isNotEmpty(user.getCloseTime())
                && user.getCloseTime().before(DateUtil.endOfDay(DateTime.now())))) {
            user = baseUserService.getUserByUsername(form.getPhone());
            if (ObjectUtil.isEmpty(user) || (ObjectUtil.isNotEmpty(user.getCloseTime())
                    && user.getCloseTime().before(DateUtil.endOfDay(DateTime.now())))) {
                user = new BaseUser();
                user.setNickName(form.getNickName());
                user.setUserName(StrUtil.isBlank(form.getAccount()) ? form.getPhone() : form.getAccount());
                user.setPassword(form.getPassword());
                user.setAvatar(form.getAvatar());
                user.setMobile(form.getPhone());
            }
            user.setMobile(form.getPhone());
            doAddUser(user);
        }
        if (ObjectUtil.isEmpty(user.getPassword())) {
            user.setPassword(form.getPassword());
        }
        if (StrUtil.isNotBlank(user.getMobile())) {
            activationMobileAccount(user);
        }
        if (StrUtil.isNotBlank(form.getAccountType())) {
            user.setUserName(form.getAccount());
            addUserThirdParty(user, form.getAccountType());
        }
        UserAccount finalAccount = doLogin(user.getUserName(), form.getAccountType());
        if (ObjectUtil.isEmpty(finalAccount)) {
            throw new ServiceException("无法完成手机号登录，请确认已绑定手机号或使用账号密码登录");
        }
        return finalAccount;
    }

    public void addUserThirdParty(BaseUser baseUser, String accountType) {
        if (!baseAccountService.isExist(baseUser.getUserName(), accountType, JbmConstants.ACCOUNT_DOMAIN_ADMIN)) {
            baseUser.setUserType(JbmConstants.USER_TYPE_ADMIN);
            baseUser.setCreateTime(new Date());
            baseUser.setUpdateTime(baseUser.getCreateTime());
            if (ObjectUtil.isEmpty(baseUser.getUserId())) {
                baseUserService.insertEntity(baseUser);
            }
            baseAccountService.register(baseUser.getUserId(), baseUser.getUserName(), baseUser.getPassword(),
                    accountType, JbmConstants.ACCOUNT_STATUS_NORMAL, JbmConstants.ACCOUNT_DOMAIN_ADMIN, null);
        }
    }

    private void doAddUser(BaseUser baseUser) {
        validationExist(baseUser);
        if (ObjectUtil.isEmpty(baseUser.getStatus())) {
            baseUser.setStatus(1);
        }
        baseUserService.insertEntity(baseUser);
        baseAccountService.register(baseUser.getUserId(), baseUser.getUserName(), baseUser.getPassword(),
                JbmConstants.ACCOUNT_TYPE_USERNAME, baseUser.getStatus(), JbmConstants.ACCOUNT_DOMAIN_ADMIN, null);
    }

    private void validationExist(BaseUser baseUser) {
        BaseUser user = baseUserService.getUserByUsername(baseUser.getUserName());
        if (ObjectUtil.isNotEmpty(user)) {
            if (ObjectUtil.isNotEmpty(user.getCloseTime()) && user.getCloseTime().before(DateUtil.endOfDay(DateTime.now()))) {
                baseUserService.deleteById(user.getUserId());
            } else {
                throw new ServiceException("用户名:" + baseUser.getUserName() + "已存在!");
            }
        }
    }

    private void activationMobileAccount(BaseUser baseUser) {
        BaseUser dbUser = baseUserService.getUserById(baseUser.getUserId());
        if (ObjectUtil.isEmpty(dbUser)) {
            throw new ServiceException("用户不存在!");
        }
        if (!Validator.isMobile(dbUser.getMobile())) {
            throw new ServiceException(AccountType.mobile.getValue() + "不符合规则！");
        }
        BaseAccount userNameAccount = baseAccountService.getAccount(dbUser.getUserName(),
                AccountType.username.toString(), JbmConstants.ACCOUNT_DOMAIN_ADMIN);
        userNameAccount.setUserId(dbUser.getUserId());
        userNameAccount.setAccountId(null);
        userNameAccount.setAccount(StrUtil.toString(dbUser.getMobile()));
        userNameAccount.setAccountType(AccountType.mobile.toString());
        baseAccountService.register(userNameAccount);
    }
}
