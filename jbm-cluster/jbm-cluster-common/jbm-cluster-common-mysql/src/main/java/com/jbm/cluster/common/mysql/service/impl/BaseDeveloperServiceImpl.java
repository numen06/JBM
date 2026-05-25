package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.api.entitys.basic.BaseDeveloper;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseRoleUser;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.common.mysql.mapper.BaseDeveloperMapper;
import com.jbm.cluster.common.mysql.mapper.BaseRoleMapper;
import com.jbm.cluster.common.mysql.mapper.BaseRoleUserMapper;
import com.jbm.cluster.common.mysql.service.BaseAccountService;
import com.jbm.cluster.common.mysql.service.BaseDeveloperService;
import com.jbm.cluster.common.mysql.service.BaseUserService;
import com.jbm.cluster.core.constant.ApiKeyConstants;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.cluster.api.form.BaseDeveloperForm;
import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.util.StringUtils;
import jbm.framework.web.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class BaseDeveloperServiceImpl extends MasterDataServiceImpl<BaseDeveloper> implements BaseDeveloperService {

    private final String ACCOUNT_DOMAIN = JbmConstants.ACCOUNT_DOMAIN_PORTAL;
    @Autowired
    private BaseDeveloperMapper baseDeveloperMapper;
    @Autowired
    private BaseAccountService baseAccountService;
    @Autowired
    private BaseUserService baseUserService;
    @Autowired
    private BaseRoleMapper baseRoleMapper;
    @Autowired
    private BaseRoleUserMapper baseRoleUserMapper;

    /**
     * 添加系统用户
     *
     * @param baseDeveloper
     * @return
     */
    @Override
    public void addUser(BaseDeveloper baseDeveloper) {
        if (getUserByUsername(baseDeveloper.getUserName()) != null) {
            throw new ServiceException("用户名:" + baseDeveloper.getUserName() + "已存在!");
        }
        baseDeveloper.setCreateTime(new Date());
        baseDeveloper.setUpdateTime(baseDeveloper.getCreateTime());
        //保存系统用户信息
        baseDeveloperMapper.insert(baseDeveloper);
        //默认注册用户名账户
        baseAccountService.register(baseDeveloper.getUserId(), baseDeveloper.getUserName(), baseDeveloper.getPassword(), JbmConstants.ACCOUNT_TYPE_USERNAME, baseDeveloper.getStatus(), ACCOUNT_DOMAIN, null);
        if (Validator.isEmail(baseDeveloper.getEmail())) {
            //注册email账号登陆
            baseAccountService.register(baseDeveloper.getUserId(), baseDeveloper.getEmail(), baseDeveloper.getPassword(), JbmConstants.ACCOUNT_TYPE_EMAIL, baseDeveloper.getStatus(), ACCOUNT_DOMAIN, null);
        }
        if (Validator.isMobile(baseDeveloper.getMobile())) {
            //注册手机号账号登陆
            baseAccountService.register(baseDeveloper.getUserId(), baseDeveloper.getMobile(), baseDeveloper.getPassword(), JbmConstants.ACCOUNT_TYPE_MOBILE, baseDeveloper.getStatus(), ACCOUNT_DOMAIN, null);
        }
    }

    /**
     * 更新系统用户
     *
     * @param baseDeveloper
     * @return
     */
    @Override
    public void updateUser(BaseDeveloper baseDeveloper) {
        if (baseDeveloper == null || baseDeveloper.getUserId() == null) {
            return;
        }
        if (baseDeveloper.getStatus() != null) {
            baseAccountService.updateStatusByUserId(baseDeveloper.getUserId(), ACCOUNT_DOMAIN, baseDeveloper.getStatus());
        }
        baseDeveloperMapper.updateById(baseDeveloper);
    }

    /**
     * 添加第三方登录用户
     *
     * @param baseDeveloper
     * @param accountType
     */
    @Override
    public void addUserThirdParty(BaseDeveloper baseDeveloper, String accountType) {
        if (!baseAccountService.isExist(baseDeveloper.getUserName(), accountType, ACCOUNT_DOMAIN)) {
            baseDeveloper.setUserType(JbmConstants.USER_TYPE_ADMIN);
            baseDeveloper.setCreateTime(new Date());
            baseDeveloper.setUpdateTime(baseDeveloper.getCreateTime());
            //保存系统用户信息
            baseDeveloperMapper.insert(baseDeveloper);
            // 注册账号信息
            baseAccountService.register(baseDeveloper.getUserId(), baseDeveloper.getUserName(), baseDeveloper.getPassword(), accountType, JbmConstants.ACCOUNT_STATUS_NORMAL, ACCOUNT_DOMAIN, null);
        }
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
    public DataPaging<BaseDeveloper> findListPage(BaseDeveloperForm form) {
        QueryWrapper<BaseDeveloper> queryWrapper = new QueryWrapper();
        queryWrapper.lambda()
                .eq(ObjectUtils.isNotEmpty(form.getUserId()), BaseDeveloper::getUserId, form.getUserId())
                .eq(ObjectUtils.isNotEmpty(form.getUserType()), BaseDeveloper::getUserType, form.getUserType())
                .eq(ObjectUtils.isNotEmpty(form.getStatus()), BaseDeveloper::getStatus, form.getStatus())
                .eq(ObjectUtils.isNotEmpty(form.getMobile()), BaseDeveloper::getMobile, form.getMobile());
        if (ObjectUtils.isNotEmpty(form.getUserName())) {
            String kw = form.getUserName();
            queryWrapper.lambda().and(w -> w.likeRight(BaseDeveloper::getUserName, kw).or().likeRight(BaseDeveloper::getNickName, kw));
        } else if (ObjectUtils.isNotEmpty(form.getNickName())) {
            queryWrapper.lambda().likeRight(BaseDeveloper::getNickName, form.getNickName());
        }
        queryWrapper.orderByDesc("create_time");
        PageForm pageForm = form.getPageForm() != null ? form.getPageForm() : new PageForm();
        return this.selectEntitys(PageParams.from(pageForm), queryWrapper);
    }

    /**
     * 查询列表
     *
     * @return
     */
    @Override
    public List<BaseDeveloper> findAllList() {
        List<BaseDeveloper> list = baseDeveloperMapper.selectList(new QueryWrapper<>());
        return list;
    }

    /**
     * 依据系统用户Id查询系统用户信息
     *
     * @param userId
     * @return
     */
    @Override
    public BaseDeveloper getUserById(Long userId) {
        return baseDeveloperMapper.selectById(userId);
    }


    /**
     * 依据登录名查询系统用户信息
     *
     * @param username
     * @return
     */
    @Override
    public BaseDeveloper getUserByUsername(String username) {
        QueryWrapper<BaseDeveloper> queryWrapper = new QueryWrapper();
        queryWrapper.lambda()
                .eq(BaseDeveloper::getUserName, username);
        BaseDeveloper saved = baseDeveloperMapper.selectOne(queryWrapper);
        return saved;
    }


    /**
     * 支持系统用户名、手机号、email登陆
     *
     * @param account
     * @return
     */
    @Override
    public UserAccount login(String account) {
        if (StringUtils.isBlank(account)) {
            return null;
        }
        Map<String, String> parameterMap = WebUtils.getParameterMap(WebUtils.getHttpServletRequest());
        // 第三方登录标识
        String loginType = parameterMap.get("login_type");
        BaseAccount baseAccount = null;
        if (StringUtils.isNotBlank(loginType)) {
            baseAccount = baseAccountService.getAccount(account, loginType, ACCOUNT_DOMAIN);
        } else {
            // 非第三方登录

            //用户名登录
            baseAccount = baseAccountService.getAccount(account, JbmConstants.ACCOUNT_TYPE_USERNAME, ACCOUNT_DOMAIN);

            // 手机号登陆
            if (Validator.isMobile(account)) {
                baseAccount = baseAccountService.getAccount(account, JbmConstants.ACCOUNT_TYPE_MOBILE, ACCOUNT_DOMAIN);
            }
            // 邮箱登陆
            if (Validator.isEmail(account)) {
                baseAccount = baseAccountService.getAccount(account, JbmConstants.ACCOUNT_TYPE_EMAIL, ACCOUNT_DOMAIN);
            }
        }
        // 获取用户详细信息
        if (baseAccount != null) {
            //添加登录日志
//            try {
//                HttpServletRequest request = WebUtils.getHttpServletRequest();
//                if (request != null) {
//                    BaseAccountLogs log = new BaseAccountLogs();
//                    log.setDomain(ACCOUNT_DOMAIN);
//                    log.setUserId(baseAccount.getUserId());
//                    log.setAccount(baseAccount.getAccount());
//                    log.setAccountId(String.valueOf(baseAccount.getAccountId()));
//                    log.setAccountType(baseAccount.getAccountType());
//                    log.setLoginIp(IpUtils.getRequestIp(request));
//                    log.setLoginAgent(request.getHeader(HttpHeaders.USER_AGENT));
//                    baseAccountService.addLoginLog(log);
//                }
//            } catch (Exception e) {
//                log.error("添加登录日志失败:{}", e);
//            }
            // 用户权限信息
            // 复制账号信息
            UserAccount userAccount = new UserAccount();
            BeanUtils.copyProperties(baseAccount, userAccount);
            return userAccount;
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyForDeveloper(Long userId, String userType) {
        if (userId == null) {
            throw new ServiceException("未登录");
        }
        BaseDeveloper existing = getUserById(userId);
        if (existing != null) {
            if (existing.getStatus() != null && existing.getStatus() == ApiKeyConstants.DEVELOPER_STATUS_ACTIVE) {
                throw new ServiceException("您已是开发者");
            }
            if (existing.getStatus() != null && existing.getStatus() == ApiKeyConstants.DEVELOPER_STATUS_PENDING) {
                throw new ServiceException("申请已提交，请等待审批");
            }
        }
        BaseUser user = baseUserService.getUserById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        Date now = new Date();
        BaseDeveloper developer = existing != null ? existing : new BaseDeveloper();
        developer.setUserId(userId);
        developer.setUserName(user.getUserName());
        developer.setNickName(user.getNickName());
        developer.setEmail(user.getEmail());
        developer.setMobile(user.getMobile());
        developer.setUserType(StrUtil.isBlank(userType) ? "dev" : userType);
        developer.setStatus(ApiKeyConstants.DEVELOPER_STATUS_PENDING);
        developer.setUpdateTime(now);
        if (existing == null) {
            developer.setCreateTime(now);
            baseDeveloperMapper.insert(developer);
        } else {
            baseDeveloperMapper.updateById(developer);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveDeveloper(Long userId) {
        BaseDeveloper developer = getUserById(userId);
        if (developer == null) {
            throw new ServiceException("开发者申请不存在");
        }
        if (developer.getStatus() != null && developer.getStatus() == ApiKeyConstants.DEVELOPER_STATUS_ACTIVE) {
            return;
        }
        developer.setStatus(ApiKeyConstants.DEVELOPER_STATUS_ACTIVE);
        developer.setUpdateTime(new Date());
        baseDeveloperMapper.updateById(developer);
        Long roleId = ensureDeveloperRoleId();
        QueryWrapper<BaseRoleUser> q = new QueryWrapper<>();
        q.lambda().eq(BaseRoleUser::getUserId, userId).eq(BaseRoleUser::getRoleId, roleId);
        if (baseRoleUserMapper.selectCount(q) == 0) {
            BaseRoleUser roleUser = new BaseRoleUser();
            roleUser.setUserId(userId);
            roleUser.setRoleId(roleId);
            roleUser.setCreateTime(new Date());
            roleUser.setUpdateTime(roleUser.getCreateTime());
            baseRoleUserMapper.insert(roleUser);
        }
    }

    @Override
    public List<BaseDeveloper> findPendingList() {
        QueryWrapper<BaseDeveloper> q = new QueryWrapper<>();
        q.lambda().eq(BaseDeveloper::getStatus, ApiKeyConstants.DEVELOPER_STATUS_PENDING);
        q.orderByDesc("create_time");
        return baseDeveloperMapper.selectList(q);
    }

    private Long ensureDeveloperRoleId() {
        QueryWrapper<BaseRole> q = new QueryWrapper<>();
        q.lambda().eq(BaseRole::getRoleCode, JbmConstants.DEVELOPER_ROLE_CODE);
        BaseRole role = baseRoleMapper.selectOne(q);
        if (role != null) {
            return role.getRoleId();
        }
        Date now = new Date();
        role = new BaseRole();
        role.setRoleId(JbmConstants.DEVELOPER_ROLE_ID);
        role.setRoleCode(JbmConstants.DEVELOPER_ROLE_CODE);
        role.setRoleName("开发者");
        role.setRoleDesc("第三方 API 开发者角色");
        role.setStatus(JbmConstants.ENABLED);
        role.setIsPersist(JbmConstants.ENABLED);
        role.setCreateTime(now);
        role.setUpdateTime(now);
        baseRoleMapper.insert(role);
        return role.getRoleId();
    }
}
