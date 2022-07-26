package com.jbm.cluster.center.service.impl;

import cn.hutool.core.lang.Validator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.api.entitys.basic.BaseDeveloper;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.center.mapper.BaseDeveloperMapper;
import com.jbm.cluster.center.service.BaseAccountService;
import com.jbm.cluster.center.service.BaseDeveloperService;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
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
@Transactional(rollbackFor = Exception.class)
public class BaseDeveloperServiceImpl extends MasterDataServiceImpl<BaseDeveloper> implements BaseDeveloperService {

    private final String ACCOUNT_DOMAIN = JbmConstants.ACCOUNT_DOMAIN_PORTAL;
    @Autowired
    private BaseDeveloperMapper baseDeveloperMapper;
    @Autowired
    private BaseAccountService baseAccountService;

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
    public DataPaging<BaseDeveloper> findListPage(PageRequestBody pageRequestBody) {
        BaseDeveloper query = pageRequestBody.tryGet(BaseDeveloper.class);
        QueryWrapper<BaseDeveloper> queryWrapper = new QueryWrapper();
        queryWrapper.lambda()
                .eq(ObjectUtils.isNotEmpty(query.getUserId()), BaseDeveloper::getUserId, query.getUserId())
                .eq(ObjectUtils.isNotEmpty(query.getUserType()), BaseDeveloper::getUserType, query.getUserType())
                .eq(ObjectUtils.isNotEmpty(query.getUserName()), BaseDeveloper::getUserName, query.getUserName())
                .eq(ObjectUtils.isNotEmpty(query.getMobile()), BaseDeveloper::getMobile, query.getMobile());
        queryWrapper.orderByDesc("create_time");
        return this.selectEntitys(pageRequestBody.getPageParams(), queryWrapper);
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
}
