package com.jbm.cluster.center.business.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.AccountType;
import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.cluster.api.form.ThirdPartyUserForm;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.center.business.BaseUserBusiness;
import com.jbm.framework.masterdata.business.BaseBusiness;
import com.jbm.cluster.common.mysql.service.BaseAccountService;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.common.mysql.service.BaseOrgService;
import com.jbm.cluster.common.mysql.service.BaseRoleService;
import com.jbm.cluster.common.mysql.service.BaseUserService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.PasswordUtils;
import com.jbm.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 平台用户业务实现：经 {@link BaseUserService} 与其它 Service 访问数据。
 */
@Slf4j
@Service
public class BaseUserBusinessImpl extends BaseBusiness implements BaseUserBusiness {

    @Autowired
    private BaseUserService baseUserService;
    @Autowired
    private BaseRoleService roleService;
    @Autowired
    private BaseOrgService orgService;
    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private BaseAccountService baseAccountService;


    @Override
    public BaseUser saveEntity(BaseUser baseUser) {
        if (ObjectUtil.isNotEmpty(baseUser.getDepartmentId())) {
            BaseOrg baseOrg = new BaseOrg();
            baseOrg.setId(baseUser.getDepartmentId());
            // 获取顶层公司
            BaseOrg rootOrg = orgService.findTopCompany(baseOrg);
            // 企业下账户数量
            Integer numberOfAccounts = ObjectUtil.defaultIfNull(rootOrg.getNumberOfAccounts(), Integer.MAX_VALUE);
            BaseUser countProbe = new BaseUser();
            countProbe.setCompanyId(rootOrg.getId());
            countProbe.setStatus(JbmConstants.ACCOUNT_STATUS_NORMAL);
            long existAccount = baseUserService.count(countProbe);
            if (NumberUtil.compare(numberOfAccounts, existAccount) != 1) {
                throw new ServiceException("企业下用户数已达上限");
            }
            baseUser.setCompanyId(rootOrg.getId());
        }
        if (ObjectUtil.isEmpty(baseUser.getUserId())) {
            doAddUser(baseUser);
        } else {
            doUpdateUser(baseUser);
        }
        return baseUser;
    }

    @Override
    public List<BaseUser> selectEntitys(BaseUserForm baseUserForm) {
        // 超级管理员账号查询所有数据
        if (ObjectUtil.isEmpty(LoginHelper.softGetLoginUser()) || LoginHelper.isAdmin()) {
            return baseUserService.selectEntitys(baseUserForm);
        }
        BaseOrg currentOrg = this.orgService.selectById(LoginHelper.getDeptId());
        if (ObjectUtil.isEmpty(currentOrg)) {
            // 用户不存在部门的情况下，仅查询自己的数据
            baseUserForm.setUserId(LoginHelper.getUserId());
            return baseUserService.selectEntitys(baseUserForm);
        }
        // 仅查询用户所属组织的数据
        BaseOrg parentOrg = this.orgService.findTopCompany(currentOrg);
        baseUserForm.setCompanyId(parentOrg.getId());
        return baseUserService.selectUserRows(baseUserForm);
    }

    @Override
    public DataPaging<BaseUser> selectEntitys(BaseUserForm baseUserForm, PageForm pageForm) {
        // 超级管理员账号查询所有数据
        if (ObjectUtil.isEmpty(LoginHelper.softGetLoginUser()) || LoginHelper.isAdmin()) {
            return baseUserService.selectEntitys(baseUserForm, pageForm);
        }
        BaseOrg currentOrg = this.orgService.selectById(LoginHelper.getDeptId());
        if (ObjectUtil.isEmpty(currentOrg)) {
            // 用户不存在部门的情况下，仅查询自己的数据
            baseUserForm.setUserId(LoginHelper.getUserId());
            return baseUserService.selectEntitys(baseUserForm, pageForm);
        }
        // 仅查询用户所属组织的数据
        BaseOrg parentOrg = this.orgService.findTopCompany(currentOrg);
        baseUserForm.setCompanyId(parentOrg.getId());
        return baseUserService.selectUserRows(baseUserForm, pageForm);
    }

    @Override
    public void register(BaseUser baseUser, String registerIp) {
        this.validationExist(baseUser);
        PasswordUtils.checkPassword(baseUser.getPassword());
        baseUser.setStatus(JbmConstants.ACCOUNT_STATUS_NORMAL);
        // 注册用户为普通管理员
        baseUser.setUserType(JbmConstants.USER_TYPE_NORMAL);
        // 保存系统用户信息
        baseUserService.insertEntity(baseUser);
        // 默认注册用户名账户
        baseAccountService.register(baseUser.getUserId(), baseUser.getUserName(), baseUser.getPassword(), JbmConstants.ACCOUNT_TYPE_USERNAME, baseUser.getStatus(), JbmConstants.ACCOUNT_DOMAIN_ADMIN, registerIp);
        if (Validator.isEmail(baseUser.getEmail())) {
            // 注册email账号登陆
            baseAccountService.register(baseUser.getUserId(), baseUser.getEmail(), baseUser.getPassword(), JbmConstants.ACCOUNT_TYPE_EMAIL, baseUser.getStatus(), JbmConstants.ACCOUNT_DOMAIN_ADMIN, registerIp);
        }
        if (Validator.isMobile(baseUser.getMobile())) {
            // 注册手机号账号登陆
            baseAccountService.register(baseUser.getUserId(), baseUser.getMobile(), baseUser.getPassword(), JbmConstants.ACCOUNT_TYPE_MOBILE, baseUser.getStatus(), JbmConstants.ACCOUNT_DOMAIN_ADMIN, registerIp);
        }
    }

    @Override
    public Boolean close(BaseUser baseUser) {
        DateTime currentDateTime = new DateTime();
        BaseUser user = baseUserService.selectById(baseUser.getUserId());
        Assert.notNull(user, () -> new ServiceException("用户不存在"));
        if (LoginHelper.isAdmin(user.getUserId())) {
            throw new ServiceException("管理员不允许注销");
        }
        if (BooleanUtil.toBoolean(ObjectUtil.isEmpty(baseUser.getStatus()) ? "0" : baseUser.getStatus().toString())) {
//        SmsNotification smsNotification = new SmsNotification();
//        smsNotification.setPhoneNumber(baseUser.getMobile());
//        smsNotification.setParams(MapUtil.of("code", code));
//        smsNotification.setSignName("甲佳智能");
//        smsNotification.setTemplateCode("SMS_236340338");
//        this.applicationContext.getBean(JbmClusterNotification.class).sendSmsNotification(smsNotification);
            user.setStatus(JbmConstants.ACCOUNT_STATUS_DISABLE);
            user.setCloseTime(currentDateTime);
            return baseUserService.updateById(user);
        } else {
            DateTime closeTime = currentDateTime.offsetNew(DateField.DAY_OF_YEAR, 7);
//        SmsNotification smsNotification = new SmsNotification();
//        smsNotification.setPhoneNumber(baseUser.getMobile());
//        smsNotification.setParams(MapUtil.of("code", code));
//        smsNotification.setSignName("甲佳智能");
//        smsNotification.setTemplateCode("SMS_236340338");
//        this.applicationContext.getBean(JbmClusterNotification.class).sendSmsNotification(smsNotification);
            user.setCloseTime(closeTime);
            return baseUserService.updateById(user);
        }
    }

    /**
     * 添加系统用户
     *
     * @param baseUser
     * @return
     */
    @Override
    public void addUser(BaseUser baseUser) {
        doAddUser(baseUser);
    }

    private void doAddUser(BaseUser baseUser) {
        validationExist(baseUser);
//        baseUser.setCreateTime(new Date());
//        baseUser.setUpdateTime(baseUser.getCreateTime());
        if (ObjectUtil.isEmpty(baseUser.getStatus())) {
            baseUser.setStatus(1);
        }
        //保存系统用户信息
        baseUserService.insertEntity(baseUser);
        //默认注册用户名账户
        baseAccountService.register(baseUser.getUserId(), baseUser.getUserName(), baseUser.getPassword(), JbmConstants.ACCOUNT_TYPE_USERNAME, baseUser.getStatus(), JbmConstants.ACCOUNT_DOMAIN_ADMIN, null);
//        if (Validator.isEmail(baseUser.getEmail())) {
//            //注册email账号登陆
//            baseAccountService.register(baseUser.getUserId(), baseUser.getEmail(), baseUser.getPassword(), JbmConstants.ACCOUNT_TYPE_EMAIL, baseUser.getStatus(), JbmConstants.ACCOUNT_DOMAIN_ADMIN, null);
//        }
//        if (Validator.isMobile(baseUser.getMobile())) {
//            //注册手机号账号登陆
//            baseAccountService.register(baseUser.getUserId(), baseUser.getMobile(), baseUser.getPassword(), JbmConstants.ACCOUNT_TYPE_MOBILE, baseUser.getStatus(), JbmConstants.ACCOUNT_DOMAIN_ADMIN, null);
//        }
    }

    private void validationExist(BaseUser baseUser) {
        BaseUser user = baseUserService.getUserByUsername(baseUser.getUserName());
        if (ObjectUtil.isNotEmpty(user)) {
            // 用户注销完成后再次创建时，删除原有用户信息
            if (ObjectUtil.isNotEmpty(user.getCloseTime()) && user.getCloseTime().before(DateUtil.endOfDay(DateTime.now()))) {
                baseUserService.deleteById(user.getUserId());
            } else {
                throw new ServiceException("用户名:" + baseUser.getUserName() + "已存在!");
            }
        }
        if (Validator.isMobile(baseUser.getUserName())) {
            BaseAccount account = this.baseAccountService.getAccount(baseUser.getUserName(), JbmConstants.ACCOUNT_TYPE_MOBILE, JbmConstants.ACCOUNT_DOMAIN_ADMIN);
            if (ObjectUtil.isNotEmpty(account) && ObjectUtil.isNotEmpty(baseUserService.selectById(account.getUserId()))) {
                throw new ServiceException("手机号:" + baseUser.getUserName() + "已存在对应用户!");
            }
        }
        if (Validator.isEmail(baseUser.getUserName())) {
            BaseAccount account = this.baseAccountService.getAccount(baseUser.getUserName(), JbmConstants.ACCOUNT_TYPE_EMAIL, JbmConstants.ACCOUNT_DOMAIN_ADMIN);
            if (ObjectUtil.isNotEmpty(account) && ObjectUtil.isNotEmpty(baseUserService.selectById(account.getUserId()))) {
                throw new ServiceException("邮箱:" + baseUser.getUserName() + "已存在对应用户!");
            }
        }
    }


    @Override
    public void activationEmailAccount(BaseUser baseUser) {
        BaseUser dbUser = baseUserService.getUserById(baseUser.getUserId());
        if (ObjectUtil.isEmpty(dbUser)) {
            throw new ServiceException("用户不存在!");
        }
        if (!Validator.isEmail(dbUser.getEmail())) {
            throw new ServiceException(AccountType.email.getValue() + "不符合规则！");
        }
        BaseAccount userNameAccount = baseAccountService.getAccount(dbUser.getUserName(), AccountType.username.toString(), JbmConstants.ACCOUNT_DOMAIN_ADMIN);
        userNameAccount.setUserId(dbUser.getUserId());
        //新建一个邮箱帐号
        userNameAccount.setAccountId(null);
        userNameAccount.setAccount(dbUser.getEmail());
        userNameAccount.setAccountType(AccountType.email.toString());
        baseAccountService.register(userNameAccount);
    }

    @Override
    public void activationMobileAccount(BaseUser baseUser) {
        BaseUser dbUser = baseUserService.getUserById(baseUser.getUserId());
        if (ObjectUtil.isEmpty(dbUser)) {
            throw new ServiceException("用户不存在!");
        }
        if (!Validator.isMobile(dbUser.getMobile())) {
            throw new ServiceException(AccountType.mobile.getValue() + "不符合规则！");
        }
        BaseAccount userNameAccount = baseAccountService.getAccount(dbUser.getUserName(), AccountType.username.toString(), JbmConstants.ACCOUNT_DOMAIN_ADMIN);
        userNameAccount.setUserId(dbUser.getUserId());
        //新建一个手机帐号
        userNameAccount.setAccountId(null);
        userNameAccount.setAccount(StrUtil.toString(dbUser.getMobile()));
        userNameAccount.setAccountType(AccountType.mobile.toString());
        baseAccountService.register(userNameAccount);
    }


    /**
     * 更新系统用户
     *
     * @param baseUser
     * @return
     */
    @Override
    public void updateUser(BaseUser baseUser) {
        doUpdateUser(baseUser);
    }

    private void doUpdateUser(BaseUser baseUser) {
        if (baseUser == null || baseUser.getUserId() == null) {
            return;
        }
        baseUserService.updateById(baseUser);
        if (baseUser.getStatus() != null) {
            if (NumberUtil.equals(baseUser.getStatus().intValue(), JbmConstants.ACCOUNT_STATUS_NORMAL)) {
                BaseUser clearClose = baseUserService.selectById(baseUser.getUserId());
                if (clearClose != null) {
                    clearClose.setCloseTime(null);
                    baseUserService.updateById(clearClose);
                }
            }
            baseAccountService.updateStatusByUserId(baseUser.getUserId(), JbmConstants.ACCOUNT_DOMAIN_ADMIN, baseUser.getStatus());
        }
    }

    /**
     * 添加第三方登录用户
     *
     * @param baseUser
     * @param accountType
     */
    @Override
    public void addUserThirdParty(BaseUser baseUser, String accountType) {
        if (!baseAccountService.isExist(baseUser.getUserName(), accountType, JbmConstants.ACCOUNT_DOMAIN_ADMIN)) {
            baseUser.setUserType(JbmConstants.USER_TYPE_ADMIN);
            baseUser.setCreateTime(new Date());
            baseUser.setUpdateTime(baseUser.getCreateTime());
//            //保存系统用户信息
//            this.saveEntity(baseUser);
            if (ObjectUtil.isEmpty(baseUser.getUserId())) {
                baseUserService.insertEntity(baseUser);
            }
            // 注册账号信息
            baseAccountService.register(baseUser.getUserId(), baseUser.getUserName(), baseUser.getPassword(), accountType, JbmConstants.ACCOUNT_STATUS_NORMAL, JbmConstants.ACCOUNT_DOMAIN_ADMIN, null);
        }
    }

    @Override
    public void bindUserThirdPartyByPhone(String phone, BaseAccount baseAccount) {
        baseAccount.setDomain(JbmConstants.ACCOUNT_DOMAIN_ADMIN);
        if (baseAccountService.isExist(baseAccount)) {
            return;
        }
        BaseUser baseUser = baseUserService.getUserByPhone(phone);
        if (ObjectUtil.isEmpty(baseUser)) {
            throw new ServiceException("没有此手机注册用户");
        }
        baseUser.setUserType(JbmConstants.USER_TYPE_ADMIN);
        baseAccount.setUserId(baseUser.getUserId());
        // 注册账号信息
        baseAccountService.register(baseAccount);
    }


    /**
     * 更新密码
     *
     * @param userId
     * @param password
     */
    @Override
    public void updatePassword(Long userId, String password) {
        baseAccountService.updatePasswordByUserId(userId, JbmConstants.ACCOUNT_DOMAIN_ADMIN, password);
    }

    /**
     * 分页查询
     *
     * @param pageRequestBody
     * @return
     */
    @Override
    public DataPaging<BaseUser> findListPage(BaseUserForm form) {
        QueryWrapper<BaseUser> queryWrapper = new QueryWrapper();
        BaseOrg currentOrg = this.orgService.selectById(LoginHelper.getDeptId());
        if (ObjectUtil.isEmpty(currentOrg)) {
            form.setUserId(LoginHelper.getUserId());
        }
        BaseOrg parentOrg = this.orgService.findTopCompany(currentOrg);
        queryWrapper.lambda().eq(BaseUser::getCompanyId, parentOrg.getId());
        queryWrapper.lambda()
                .eq(ObjectUtils.isNotEmpty(form.getUserId()), BaseUser::getUserId, form.getUserId())
                .eq(ObjectUtils.isNotEmpty(form.getUserType()), BaseUser::getUserType, form.getUserType())
                .eq(ObjectUtils.isNotEmpty(form.getUserName()), BaseUser::getUserName, form.getUserName())
                .eq(ObjectUtils.isNotEmpty(form.getMobile()), BaseUser::getMobile, form.getMobile());
        queryWrapper.orderByDesc("create_time");
        PageForm pageForm = form.getPageForm() != null ? form.getPageForm() : new PageForm();
        return baseUserService.selectEntitys(PageParams.from(pageForm), queryWrapper);
    }

    /**
     * 根据用户ID获取用户信息和权限
     *
     * @param userId
     * @return
     */
    @Override
    public UserAccount getUserAccount(Long userId) {
        // 用户权限列表
        List<OpenAuthority> authorities = Lists.newArrayList();
        // 用户角色列表
//        List<Map> roles = Lists.newArrayList();
        List<BaseRole> rolesList = roleService.getUserRoles(userId);
        if (rolesList != null) {
            for (BaseRole role : rolesList) {
                // 用户角色详情
//                roles.add(BeanUtil.beanToMap(role));
                // 加入角色标识
                OpenAuthority authority = new OpenAuthority(role.getRoleId().toString(), JbmSecurityConstants.AUTHORITY_PREFIX_ROLE + role.getRoleCode(), null, "role");
                authorities.add(authority);
            }
        }

        //查询系统用户资料
        BaseUser baseUser = baseUserService.getUserById(userId);

        if (NumberUtil.equals(baseUser.getStatus().intValue(), JbmConstants.ACCOUNT_STATUS_DISABLE)) {
//            throw new ServiceException("用户已停用，请联系管理员");
            if (ObjectUtil.isNotEmpty(baseUser.getCloseTime()) && baseUser.getCloseTime().before(DateUtil.endOfDay(DateTime.now()))) {
                throw new ServiceException("没有找到此用户");
            }
            throw new ServiceException("用户已停用，请联系管理员");
        }

        // 加入用户权限
        List<OpenAuthority> userGrantedAuthority = baseAuthorityService.findAuthorityByUser(userId, JbmConstants.ROOT.equals(baseUser.getUserName()));
        if (userGrantedAuthority != null && userGrantedAuthority.size() > 0) {
            authorities.addAll(userGrantedAuthority);
        }
        UserAccount userAccount = new UserAccount();
        //复制用户属性
        BeanUtil.copyProperties(baseUser, userAccount);
//        // 昵称
//        userAccount.setNickName(baseUser.getNickName());
//        // 头像
//        userAccount.setAvatar(baseUser.getAvatar());
//        // 权限信息
        userAccount.setAuthorities(authorities);
        userAccount.setRoles(rolesList);
        return userAccount;
    }

    @Override
    public UserAccount login(String account) {
        return doLogin(account, null);
    }

    /**
     * 支持系统用户名、手机号、email登陆
     *
     * @param account
     * @return
     */
    @Override
    public UserAccount login(String account, String loginType) {
        return doLogin(account, loginType);
    }

    private UserAccount doLogin(String account, String loginType) {
        if (StringUtils.isBlank(account)) {
            return null;
        }
        // 第三方登录标识
        BaseAccount baseAccount = null;
        if (StringUtils.isNotBlank(loginType)) {
            baseAccount = baseAccountService.getAccount(account, loginType, JbmConstants.ACCOUNT_DOMAIN_ADMIN);
        } else {
            // 非第三方登录
            //用户名登录
            baseAccount = baseAccountService.getAccount(account, JbmConstants.ACCOUNT_TYPE_USERNAME, JbmConstants.ACCOUNT_DOMAIN_ADMIN);
            // 手机号登陆
            if (ObjectUtil.isEmpty(baseAccount) && Validator.isMobile(account)) {
                baseAccount = baseAccountService.getAccount(account, JbmConstants.ACCOUNT_TYPE_MOBILE, JbmConstants.ACCOUNT_DOMAIN_ADMIN);
            }
            // 邮箱登陆
            if (ObjectUtil.isEmpty(baseAccount) && Validator.isEmail(account)) {
                baseAccount = baseAccountService.getAccount(account, JbmConstants.ACCOUNT_TYPE_EMAIL, JbmConstants.ACCOUNT_DOMAIN_ADMIN);
            }
        }
        // 获取用户详细信息
        if (baseAccount != null) {
            //添加登录日志
//            try {
//                HttpServletRequest request = WebUtils.getHttpServletRequest();
//                if (request != null) {
//                    BaseAccountLogs log = new BaseAccountLogs();
//                    log.setDomain(JbmConstants.ACCOUNT_DOMAIN_ADMIN);
//                    log.setUserId(baseAccount.getUserId());
//                    log.setAccount(baseAccount.getAccount());
////                    log.setAccountId(String.valueOf(baseAccount.getAccountId()));
//                    log.setAccountType(baseAccount.getAccountType());
//                    log.setLoginIp(IpUtils.getRequestIp(request));
//                    log.setLoginAgent(request.getHeader(HttpHeaders.USER_AGENT));
//                    UserAgent userAgent = UserAgentUtil.parse(log.getLoginAgent());
//                    log.setBrowser(userAgent.getBrowser().getName() + " " + userAgent.getVersion());
//                    log.setOs(userAgent.getOs().getName());
//                    baseAccountService.addLoginLog(log);
//                }
//            } catch (Exception e) {
//                log.error("添加登录日志失败:{}", e);
//            }

            // 查询系统用户资料
            BaseUser baseUser = baseUserService.getUserById(baseAccount.getUserId());
            if (ObjectUtil.isEmpty(baseUser) || ObjectUtil.isNotEmpty(baseUser.getCloseTime()) && baseUser.getCloseTime().before(DateUtil.endOfDay(DateTime.now()))) {
                return null;
            }
            // 用户权限信息
            UserAccount userAccount = getUserAccount(baseAccount.getUserId());
            // 复制账号信息
            BeanUtils.copyProperties(baseAccount, userAccount);
            userAccount.setAccountType(baseAccount.getAccountType());
            return userAccount;
        }
        return null;
    }

    @Override
    public List<BaseUser> retrievalUsers(String keyword) {
        QueryWrapper<BaseUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .and(w -> w.like(BaseUser::getUserName, keyword)
                        .or().like(BaseUser::getRealName, keyword)
                        .or().like(BaseUser::getMobile, keyword))
                .last("limit 10");
        if (!cn.dev33.satoken.stp.StpUtil.isLogin()) {
            return baseUserService.selectEntitys(queryWrapper);
        }
        BaseOrg currentOrg = this.orgService.selectById(LoginHelper.getDeptId());
        if (ObjectUtil.isEmpty(currentOrg)) {
            Long uid = LoginHelper.getUserId();
            if (uid != null) {
                queryWrapper.lambda().eq(BaseUser::getUserId, uid);
            }
            return baseUserService.selectEntitys(queryWrapper);
        }
        BaseOrg parentOrg = this.orgService.findTopCompany(currentOrg);
        if (parentOrg != null && parentOrg.getId() != null) {
            queryWrapper.lambda().eq(BaseUser::getCompanyId, parentOrg.getId());
        }
        return baseUserService.selectEntitys(queryWrapper);
    }

    @Override
    public UserAccount registerAccountByPhone(String phone, String username, String password, String accountType) {
        ThirdPartyUserForm thirdPartyUserForm = new ThirdPartyUserForm();
        thirdPartyUserForm.setPhone(phone);
        thirdPartyUserForm.setAccountType(accountType);
        thirdPartyUserForm.setAccount(username);
        thirdPartyUserForm.setPassword(password);
        return loginAndRegisterMobileUser(thirdPartyUserForm);
    }

    @Override
    public UserAccount loginAndRegisterMobileUser(ThirdPartyUserForm thirdPartyUserForm) {
        try {
            UserAccount userAccount = doLogin(thirdPartyUserForm.getAccount(), "mobile");
            if (ObjectUtil.isNotEmpty(userAccount)) {
                return userAccount;
            }
            //没有手机号不能进行注册绑定
            if (StrUtil.isBlank(thirdPartyUserForm.getPhone())) {
                throw new ServiceException("手机为空");
            }
            BaseUser user = baseUserService.getUserByPhone(thirdPartyUserForm.getPhone());
            if (ObjectUtil.isEmpty(user) || (ObjectUtil.isNotEmpty(user.getCloseTime()) && user.getCloseTime().before(DateUtil.endOfDay(DateTime.now())))) {
                user = baseUserService.getUserByUsername(thirdPartyUserForm.getPhone());
                if (ObjectUtil.isEmpty(user) || (ObjectUtil.isNotEmpty(user.getCloseTime()) && user.getCloseTime().before(DateUtil.endOfDay(DateTime.now())))) {
                    user = new BaseUser();
                    user.setNickName(thirdPartyUserForm.getNickName());
                    user.setUserName(StrUtil.isBlank(thirdPartyUserForm.getAccount()) ? thirdPartyUserForm.getPhone() : thirdPartyUserForm.getAccount());
                    user.setPassword(thirdPartyUserForm.getPassword());
                    user.setAvatar(thirdPartyUserForm.getAvatar());
                    user.setMobile(thirdPartyUserForm.getPhone());
                }
                user.setMobile(thirdPartyUserForm.getPhone());
                saveEntity(user);
            }
            if (ObjectUtil.isEmpty(user.getPassword())) {
                user.setPassword(thirdPartyUserForm.getPassword());
            }
            //如果手机号不为空自动激活手机号登录
            if (StrUtil.isNotBlank(user.getMobile())) {
                activationMobileAccount(user);
            }
            //如果是第三方来源增加第三方账号
            if (StrUtil.isNotBlank(thirdPartyUserForm.getAccountType())) {
                user.setUserName(thirdPartyUserForm.getAccount());
                addUserThirdParty(user, thirdPartyUserForm.getAccountType());
            }
            //最后再登录一次
            UserAccount finalAccount = doLogin(user.getUserName(), thirdPartyUserForm.getAccountType());
            if (ObjectUtil.isEmpty(finalAccount)) {
                throw new ServiceException("无法完成手机号登录，请确认已绑定手机号或使用账号密码登录");
            }
            return finalAccount;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("手机号登录处理失败", e);
            throw ServiceException.of(e, "手机号登录处理失败：{}", StrUtil.emptyToDefault(e.getMessage(), e.getClass().getSimpleName()));
        }
    }

    @Override
    public List<BaseRole> getUserRoles(Long userId) {
        List<BaseRole> roles = this.roleService.getUserRoles(userId);
        if (ObjectUtil.isEmpty(LoginHelper.softGetLoginUser()) || LoginHelper.isAdmin()) {
            return roles;
        }
        // 仅返回当前用户拥有的角色
        Set<Long> currentUserRoleIds = LoginHelper.getLoginUser().getRoleIds();
        return roles.stream().filter(role -> currentUserRoleIds.contains(role.getRoleId())).collect(Collectors.toList());
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        List<Long> roleIds = this.roleService.getUserRoleIds(userId);
        if (ObjectUtil.isEmpty(LoginHelper.softGetLoginUser()) || LoginHelper.isAdmin()) {
            return roleIds;
        }
        // 仅返回当前用户拥有的角色
        Set<Long> currentUserRoleIds = LoginHelper.getLoginUser().getRoleIds();
        return roleIds.stream().filter(role -> currentUserRoleIds.contains(role)).collect(Collectors.toList());
    }

}
