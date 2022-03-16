package com.jbm.cluster.center.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jbm.cluster.api.constants.AccountType;
import com.jbm.cluster.api.constants.BaseConstants;
import com.jbm.cluster.api.form.ThirdPartyUserForm;
import com.jbm.cluster.api.model.UserAccount;
import com.jbm.cluster.api.model.entity.*;
import com.jbm.cluster.center.mapper.BaseUserMapper;
import com.jbm.cluster.center.service.*;
import com.jbm.cluster.common.constants.CommonConstants;
import com.jbm.cluster.common.security.OpenAuthority;
import com.jbm.cluster.common.security.OpenSecurityConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.mvc.WebUtils;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    private final String ACCOUNT_DOMAIN = BaseConstants.ACCOUNT_DOMAIN_ADMIN;

    @Override
    public BaseUser saveEntity(BaseUser baseUser) {
        if (ObjectUtil.isNotEmpty(baseUser.getDepartmentId())) {
            BaseOrg baseOrg = new BaseOrg();
            baseOrg.setId(baseUser.getDepartmentId());
            //查询上级公司
            BaseOrg rCompany = orgService.findRelegationCompany(baseOrg);
            baseUser.setCompanyId(rCompany.getId());
        }
        if (ObjectUtil.isEmpty(baseUser.getUserId())) {
            this.addUser(baseUser);
        } else {
            this.updateUser(baseUser);
        }
        return baseUser;
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
//        baseUser.setCreateTime(new Date());
//        baseUser.setUpdateTime(baseUser.getCreateTime());
        if (ObjectUtil.isEmpty(baseUser.getStatus())) {
            baseUser.setStatus(1);
        }
        //保存系统用户信息
        baseUserMapper.insert(baseUser);
        //默认注册用户名账户
        baseAccountService.register(baseUser.getUserId(), baseUser.getUserName(), baseUser.getPassword(), BaseConstants.ACCOUNT_TYPE_USERNAME, baseUser.getStatus(), ACCOUNT_DOMAIN, null);
//        if (Validator.isEmail(baseUser.getEmail())) {
//            //注册email账号登陆
//            baseAccountService.register(baseUser.getUserId(), baseUser.getEmail(), baseUser.getPassword(), BaseConstants.ACCOUNT_TYPE_EMAIL, baseUser.getStatus(), ACCOUNT_DOMAIN, null);
//        }
//        if (Validator.isMobile(baseUser.getMobile())) {
//            //注册手机号账号登陆
//            baseAccountService.register(baseUser.getUserId(), baseUser.getMobile(), baseUser.getPassword(), BaseConstants.ACCOUNT_TYPE_MOBILE, baseUser.getStatus(), ACCOUNT_DOMAIN, null);
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
        BaseAccount userNameAccunt = baseAccountService.getAccount(dbUser.getUserName(), AccountType.username.toString(), ACCOUNT_DOMAIN);
        //新建一个邮箱帐号
        userNameAccunt.setAccountId(null);
        userNameAccunt.setAccountType(AccountType.email.toString());
        baseAccountService.register(userNameAccunt);
    }

    @Override
    public void activationMobileAccount(BaseUser baseUser) {
        BaseUser dbUser = this.getUserById(baseUser.getUserId());
        if (ObjectUtil.isEmpty(dbUser)) {
            throw new ServiceException("用户不存在!");
        }
        if (!Validator.isMobile(dbUser.getMobile())) {
            throw new ServiceException(AccountType.email.getValue() + "不符合规则！");
        }
        BaseAccount userNameAccunt = baseAccountService.getAccount(dbUser.getUserName(), AccountType.username.toString(), ACCOUNT_DOMAIN);
        //新建一个手机帐号
        userNameAccunt.setAccountId(null);
        userNameAccunt.setAccountType(AccountType.mobile.toString());
        baseAccountService.register(userNameAccunt);
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
            baseAccountService.updateStatusByUserId(baseUser.getUserId(), ACCOUNT_DOMAIN, baseUser.getStatus());
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
        if (!baseAccountService.isExist(baseUser.getUserName(), accountType, ACCOUNT_DOMAIN)) {
            baseUser.setUserType(BaseConstants.USER_TYPE_ADMIN);
            baseUser.setCreateTime(new Date());
            baseUser.setUpdateTime(baseUser.getCreateTime());
//            //保存系统用户信息
//            this.saveEntity(baseUser);
            if (ObjectUtil.isEmpty(baseUser.getUserId())) {
                this.insertEntity(baseUser);
            }
            // 注册账号信息
            baseAccountService.register(baseUser.getUserId(), baseUser.getUserName(), baseUser.getPassword(), accountType, BaseConstants.ACCOUNT_STATUS_NORMAL, ACCOUNT_DOMAIN, null);
        }
    }

    @Override
    public void bindUserThirdPartyByPhone(String phone, BaseAccount baseAccount) {
        baseAccount.setDomain(BaseConstants.ACCOUNT_DOMAIN_ADMIN);
        if (baseAccountService.isExist(baseAccount)) {
            return;
        }
        BaseUser baseUser = this.getUserByPhone(phone);
        if (ObjectUtil.isEmpty(baseUser)) {
            throw new ServiceException("没有此手机注册用户");
        }
        baseUser.setUserType(BaseConstants.USER_TYPE_ADMIN);
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
        baseAccountService.updatePasswordByUserId(userId, ACCOUNT_DOMAIN, password);
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
        queryWrapper.lambda()
                .eq(ObjectUtils.isNotEmpty(query.getUserId()), BaseUser::getUserId, query.getUserId())
                .eq(ObjectUtils.isNotEmpty(query.getUserType()), BaseUser::getUserType, query.getUserType())
                .eq(ObjectUtils.isNotEmpty(query.getUserName()), BaseUser::getUserName, query.getUserName())
                .eq(ObjectUtils.isNotEmpty(query.getMobile()), BaseUser::getMobile, query.getMobile());
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
        List<Map> roles = Lists.newArrayList();
        List<BaseRole> rolesList = roleService.getUserRoles(userId);
        if (rolesList != null) {
            for (BaseRole role : rolesList) {
                Map roleMap = Maps.newHashMap();
                roleMap.put("roleId", role.getRoleId());
                roleMap.put("roleCode", role.getRoleCode());
                roleMap.put("roleName", role.getRoleName());
                // 用户角色详情
                roles.add(roleMap);
                // 加入角色标识
                OpenAuthority authority = new OpenAuthority(role.getRoleId().toString(), OpenSecurityConstants.AUTHORITY_PREFIX_ROLE + role.getRoleCode(), null, "role");
                authorities.add(authority);
            }
        }

        //查询系统用户资料
        BaseUser baseUser = getUserById(userId);

        // 加入用户权限
        List<OpenAuthority> userGrantedAuthority = baseAuthorityService.findAuthorityByUser(userId, CommonConstants.ROOT.equals(baseUser.getUserName()));
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
//        userAccount.setAuthorities(authorities);
        userAccount.setRoles(roles);
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
        queryWrapper.lambda()
                .eq(BaseUser::getUserName, username);
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
            baseAccount = baseAccountService.getAccount(account, loginType, ACCOUNT_DOMAIN);
        } else {
            // 非第三方登录
            //用户名登录
            baseAccount = baseAccountService.getAccount(account, BaseConstants.ACCOUNT_TYPE_USERNAME, ACCOUNT_DOMAIN);
            // 手机号登陆
            if (Validator.isMobile(account)) {
                baseAccount = baseAccountService.getAccount(account, BaseConstants.ACCOUNT_TYPE_MOBILE, ACCOUNT_DOMAIN);
            }
            // 邮箱登陆
            if (Validator.isEmail(account)) {
                baseAccount = baseAccountService.getAccount(account, BaseConstants.ACCOUNT_TYPE_EMAIL, ACCOUNT_DOMAIN);
            }
        }

        // 获取用户详细信息
        if (baseAccount != null) {
            //添加登录日志
            try {
                HttpServletRequest request = WebUtils.getHttpServletRequest();
                if (request != null) {
                    BaseAccountLogs log = new BaseAccountLogs();
                    log.setDomain(ACCOUNT_DOMAIN);
                    log.setUserId(baseAccount.getUserId());
                    log.setAccount(baseAccount.getAccount());
                    log.setAccountId(String.valueOf(baseAccount.getAccountId()));
                    log.setAccountType(baseAccount.getAccountType());
                    log.setLoginIp(WebUtils.getRemoteAddress(request));
                    log.setLoginAgent(request.getHeader(HttpHeaders.USER_AGENT));
                    UserAgent userAgent = UserAgentUtil.parse(log.getLoginAgent());
                    log.setBrowser(userAgent.getBrowser().getName() + " " + userAgent.getVersion());
                    log.setOs(userAgent.getOs().getName());
                    baseAccountService.addLoginLog(log);
                }
            } catch (Exception e) {
                log.error("添加登录日志失败:{}", e);
            }
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
        queryWrapper.lambda()
                .or().like(BaseUser::getRealName, keyword)
                .or().like(BaseUser::getMobile, keyword)
                .or().like(BaseUser::getNickName, keyword).last("limit 10");
        return this.selectEntitys(queryWrapper);
    }

    @Override
    public UserAccount loginAndRegisterMobileUser(ThirdPartyUserForm thirdPartyUserForm) {
        try {
            UserAccount userAccount = this.login(thirdPartyUserForm.getAccount(), thirdPartyUserForm.getPassword());
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
                //新增用户
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
            return this.login(user.getUserName(), thirdPartyUserForm.getPassword());
        } catch (Exception e) {
            log.error("添加登录日志失败:{}", e);
            throw new ServiceException(e);
        }
    }

}
