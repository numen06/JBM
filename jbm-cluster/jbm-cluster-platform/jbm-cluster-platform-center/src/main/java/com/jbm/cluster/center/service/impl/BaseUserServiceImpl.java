package com.jbm.cluster.center.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Validator;
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
import com.jbm.cluster.center.mapper.BaseUserMapper;
import com.jbm.cluster.center.service.*;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.PasswordUtils;
import com.jbm.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: wesley.zhang
 * @date: 2018/10/24 16:33
 * @description:
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseUserServiceImpl extends MasterDataServiceImpl<BaseUser> implements BaseUserService {

    @Autowired
    private BaseUserMapper baseUserMapper;
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
            long existAccount = this.count(new QueryWrapper<BaseUser>().lambda().eq(BaseUser::getCompanyId, rootOrg.getId()));
            if (NumberUtil.compare(rootOrg.getNumberOfAccounts(), existAccount) != 1) {
                throw new ServiceException("企业下用户数已满");
            }
            baseUser.setCompanyId(rootOrg.getId());
        }
        if (ObjectUtil.isEmpty(baseUser.getUserId())) {
            this.addUser(baseUser);
        } else {
            this.updateUser(baseUser);
        }
        return baseUser;
    }

    @Override
    public List<BaseUser> selectEntitys(BaseUserForm baseUserForm) {
        // 超级管理员账号查询所有数据
        if (LoginHelper.isAdmin()) {
            return super.selectEntitys(baseUserForm);
        }
        BaseOrg currentOrg = this.orgService.selectById(LoginHelper.getDeptId());
        if (ObjectUtil.isEmpty(currentOrg)) {
            // 用户不存在部门的情况下，仅查询自己的数据
            baseUserForm.setUserId(LoginHelper.getUserId());
            return super.selectEntitys(baseUserForm);
        }
        // 仅查询用户所属组织的数据
        BaseOrg parentOrg = this.orgService.findTopCompany(currentOrg);
        baseUserForm.setCompanyId(parentOrg.getId());
        return this.baseUserMapper.selectData(baseUserForm);
    }

    @Override
    public DataPaging<BaseUser> selectEntitys(BaseUserForm baseUserForm, PageForm pageForm) {
        // 超级管理员账号查询所有数据
        if (LoginHelper.isAdmin()) {
            return super.selectEntitys(baseUserForm, pageForm);
        }
        BaseOrg currentOrg = this.orgService.selectById(LoginHelper.getDeptId());
        if (ObjectUtil.isEmpty(currentOrg)) {
            // 用户不存在部门的情况下，仅查询自己的数据
            baseUserForm.setUserId(LoginHelper.getUserId());
            return super.selectEntitys(baseUserForm, pageForm);
        }
        // 仅查询用户所属组织的数据
        BaseOrg parentOrg = this.orgService.findTopCompany(currentOrg);
        baseUserForm.setCompanyId(parentOrg.getId());
        return super.selectPageList(pageForm, (page) -> this.baseUserMapper.selectData(baseUserForm, page));
    }

    @Override
    public void register(BaseUser baseUser, String registerIp) {
        if (getUserByUsername(baseUser.getUserName()) != null) {
            throw new ServiceException("用户名:" + baseUser.getUserName() + "已存在!");
        }
        PasswordUtils.checkPassword(baseUser.getPassword());
        baseUser.setStatus(JbmConstants.ACCOUNT_STATUS_NORMAL);
        // 注册用户为普通管理员
        baseUser.setUserType(JbmConstants.USER_TYPE_NORMAL);
        // 保存系统用户信息
        baseUserMapper.insert(baseUser);
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

    /**
     * 添加系统用户
     *
     * @param baseUser
     * @return
     */
    @Override
    public void addUser(BaseUser baseUser) {
        if (getUserByUsername(baseUser.getUserName()) != null) {
            throw new ServiceException("用户名:" + baseUser.getUserName() + "已存在!");
        }
        if (Validator.isMobile(baseUser.getUserName()) && baseAccountService.isExist(baseUser.getUserName(), JbmConstants.ACCOUNT_TYPE_MOBILE, JbmConstants.ACCOUNT_DOMAIN_ADMIN)) {
            throw new ServiceException("手机号:" + baseUser.getUserName() + "已存在对应用户!");
        }
        if (Validator.isEmail(baseUser.getUserName()) && baseAccountService.isExist(baseUser.getUserName(), JbmConstants.ACCOUNT_TYPE_EMAIL, JbmConstants.ACCOUNT_DOMAIN_ADMIN)) {
            throw new ServiceException("邮箱:" + baseUser.getUserName() + "已存在对应用户!");
        }
//        baseUser.setCreateTime(new Date());
//        baseUser.setUpdateTime(baseUser.getCreateTime());
        if (ObjectUtil.isEmpty(baseUser.getStatus())) {
            baseUser.setStatus(1);
        }
        //保存系统用户信息
        baseUserMapper.insert(baseUser);
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


    @Override
    public void activationEmailAccount(BaseUser baseUser) {
        BaseUser dbUser = this.getUserById(baseUser.getUserId());
        if (ObjectUtil.isEmpty(dbUser)) {
            throw new ServiceException("用户不存在!");
        }
        if (!Validator.isEmail(dbUser.getEmail())) {
            throw new ServiceException(AccountType.email.getValue() + "不符合规则！");
        }
        BaseAccount userNameAccount = baseAccountService.getAccount(dbUser.getUserName(), AccountType.username.toString(), JbmConstants.ACCOUNT_DOMAIN_ADMIN);
        //新建一个邮箱帐号
        userNameAccount.setAccountId(null);
        userNameAccount.setAccount(dbUser.getEmail());
        userNameAccount.setAccountType(AccountType.email.toString());
        baseAccountService.register(userNameAccount);
    }

    @Override
    public void activationMobileAccount(BaseUser baseUser) {
        BaseUser dbUser = this.getUserById(baseUser.getUserId());
        if (ObjectUtil.isEmpty(dbUser)) {
            throw new ServiceException("用户不存在!");
        }
        if (!Validator.isMobile(dbUser.getMobile())) {
            throw new ServiceException(AccountType.mobile.getValue() + "不符合规则！");
        }
        BaseAccount userNameAccount = baseAccountService.getAccount(dbUser.getUserName(), AccountType.username.toString(), JbmConstants.ACCOUNT_DOMAIN_ADMIN);
        if (userNameAccount == null) {
            userNameAccount = baseAccountService.registerUsernameAccount(baseUser);
        }
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
        if (baseUser == null || baseUser.getUserId() == null) {
            return;
        }
        if (baseUser.getStatus() != null) {
            baseAccountService.updateStatusByUserId(baseUser.getUserId(), JbmConstants.ACCOUNT_DOMAIN_ADMIN, baseUser.getStatus());
        }
//        this.saveEntity(baseUser);
        baseUserMapper.updateById(baseUser);
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
                this.insertEntity(baseUser);
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
        BaseUser baseUser = this.getUserByPhone(phone);
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
    public DataPaging<BaseUser> findListPage(PageRequestBody pageRequestBody) {
        BaseUser query = pageRequestBody.tryGet(BaseUser.class);
        QueryWrapper<BaseUser> queryWrapper = new QueryWrapper();
        BaseOrg currentOrg = this.orgService.selectById(LoginHelper.getDeptId());
        if (ObjectUtil.isEmpty(currentOrg)) {
            // 用户不存在部门的情况下，仅查询自己的数据
            query.setUserId(LoginHelper.getUserId());
        }
        // 仅查询用户所属组织的数据
        BaseOrg parentOrg = this.orgService.findTopCompany(currentOrg);
        queryWrapper.lambda().eq(BaseUser::getCompanyId, parentOrg.getId());
        queryWrapper.lambda().eq(ObjectUtils.isNotEmpty(query.getUserId()), BaseUser::getUserId, query.getUserId()).eq(ObjectUtils.isNotEmpty(query.getUserType()), BaseUser::getUserType, query.getUserType()).eq(ObjectUtils.isNotEmpty(query.getUserName()), BaseUser::getUserName, query.getUserName()).eq(ObjectUtils.isNotEmpty(query.getMobile()), BaseUser::getMobile, query.getMobile());
        queryWrapper.orderByDesc("create_time");
        return this.selectEntitys(pageRequestBody.getPageParams(), queryWrapper);
    }

    /**
     * 查询列表
     *
     * @return
     */
    @Override
    public List<BaseUser> findAllList() {
        List<BaseUser> list = baseUserMapper.selectList(new QueryWrapper<>());
        return list;
    }

    /**
     * 依据系统用户Id查询系统用户信息
     *
     * @param userId
     * @return
     */
    @Override
    public BaseUser getUserById(Long userId) {
        return baseUserMapper.selectById(userId);
    }

    @Override
    public BaseUser getUserByPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return null;
        }
        BaseUser baseUser = new BaseUser();
        baseUser.setMobile(phone);
        return this.selectEntity(baseUser);
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
        BaseUser baseUser = getUserById(userId);

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


    /**
     * 依据登录名查询系统用户信息
     *
     * @param username
     * @return
     */
    @Override
    public BaseUser getUserByUsername(String username) {
        QueryWrapper<BaseUser> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(BaseUser::getUserName, username);
        BaseUser saved = baseUserMapper.selectOne(queryWrapper);
        return saved;
    }

    @Override
    public UserAccount login(String account) {
        return this.login(account, null);
    }

    /**
     * 支持系统用户名、手机号、email登陆
     *
     * @param account
     * @return
     */
    @Override
    public UserAccount login(String account, String loginType) {
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
            // 用户权限信息
            UserAccount userAccount = getUserAccount(baseAccount.getUserId());
            // 复制账号信息
            BeanUtils.copyProperties(baseAccount, userAccount);
            return userAccount;
        }
        return null;
    }

    @Override
    public List<BaseUser> retrievalUsers(String keyword) {
        QueryWrapper<BaseUser> queryWrapper = new QueryWrapper();
        BaseOrg currentOrg = this.orgService.selectById(LoginHelper.getDeptId());
        if (ObjectUtil.isEmpty(currentOrg)) {
            // 用户不存在部门的情况下，仅查询自己的数据
            queryWrapper.lambda().eq(BaseUser::getUserId, LoginHelper.getUserId());
        }
        // 仅查询用户所属组织的数据
        BaseOrg parentOrg = this.orgService.findTopCompany(currentOrg);
        queryWrapper.lambda().eq(BaseUser::getCompanyId, parentOrg.getId());
        queryWrapper.lambda().like(BaseUser::getRealName, keyword).or().like(BaseUser::getMobile, keyword).last("limit 10");
        return this.selectEntitys(queryWrapper);
    }

    @Override
    public UserAccount registerAccountByPhone(String phone, String username, String password, String accountType) {
        ThirdPartyUserForm thirdPartyUserForm = new ThirdPartyUserForm();
        thirdPartyUserForm.setPhone(phone);
        thirdPartyUserForm.setAccountType(accountType);
        thirdPartyUserForm.setAccount(username);
        thirdPartyUserForm.setPassword(password);
        return this.loginAndRegisterMobileUser(thirdPartyUserForm);
    }

    @Override
    public UserAccount loginAndRegisterMobileUser(ThirdPartyUserForm thirdPartyUserForm) {
        try {
            UserAccount userAccount = this.login(thirdPartyUserForm.getAccount(), "mobile");
            if (ObjectUtil.isNotEmpty(userAccount)) {
                return userAccount;
            }
            //没有手机号不能进行注册绑定
            if (StrUtil.isBlank(thirdPartyUserForm.getPhone())) {
                throw new ServiceException("手机为空");
            }
            BaseUser user = this.getUserByPhone(thirdPartyUserForm.getPhone());
            if (ObjectUtil.isEmpty(user)) {
                user = new BaseUser();
                user.setNickName(thirdPartyUserForm.getNickName());
                user.setUserName(thirdPartyUserForm.getAccount());
                user.setPassword(thirdPartyUserForm.getPassword());
                user.setAvatar(thirdPartyUserForm.getAvatar());
                user.setMobile(thirdPartyUserForm.getPhone());
                // 新增用户
                this.addUser(user);
            }
            if (ObjectUtil.isEmpty(user.getPassword())) {
                user.setPassword(thirdPartyUserForm.getPassword());
            }
            //如果手机号不为空自动激活手机号登录
            if (StrUtil.isNotBlank(user.getMobile())) {
                this.activationMobileAccount(user);
            }
            //如果是第三方来源增加第三方账号
            if (StrUtil.isNotBlank(thirdPartyUserForm.getAccountType())) {
                user.setUserName(thirdPartyUserForm.getAccount());
                this.addUserThirdParty(user, thirdPartyUserForm.getAccountType());
            }
            //最后再登录一次
            return this.login(user.getUserName(), thirdPartyUserForm.getAccountType());
        } catch (Exception e) {
            log.error("添加登录日志失败:{}", e);
            throw new ServiceException(e);
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
