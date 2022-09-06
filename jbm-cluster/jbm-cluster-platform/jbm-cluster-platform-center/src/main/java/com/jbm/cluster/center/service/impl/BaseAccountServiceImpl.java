package com.jbm.cluster.center.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.constants.AccountStatus;
import com.jbm.cluster.api.constants.AccountType;
import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.api.entitys.basic.BaseAccountLogs;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.center.mapper.BaseAccountLogsMapper;
import com.jbm.cluster.center.mapper.BaseAccountMapper;
import com.jbm.cluster.center.service.BaseAccountService;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.util.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 通用账号
 *
 * @author wesley.zhang
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseAccountServiceImpl extends MasterDataServiceImpl<BaseAccount> implements BaseAccountService {

    @Autowired
    private BaseAccountMapper baseAccountMapper;
    @Autowired
    private BaseAccountLogsMapper baseAccountLogsMapper;

    /**
     * 根据主键获取账号信息
     *
     * @param accountId
     * @return
     */
    @Override
    public BaseAccount getAccountById(Long accountId) {
        return baseAccountMapper.selectById(accountId);
    }

    @Override
    public List<BaseAccount> getUserAccounts(Long userId) {
        QueryWrapper<BaseAccount> wrapper = new QueryWrapper();
        wrapper.lambda()
                .eq(BaseAccount::getUserId, userId);
        return baseAccountMapper.selectList(wrapper);
    }

    /**
     * 获取账号信息
     *
     * @param account
     * @param accountType
     * @param domain
     * @return
     */
    @Override
    public BaseAccount getAccount(String account, String accountType, String domain) {
        QueryWrapper<BaseAccount> queryWrapper = new QueryWrapper();
        queryWrapper.lambda()
                .eq(BaseAccount::getAccount, account)
                .eq(BaseAccount::getAccountType, accountType)
                .eq(BaseAccount::getDomain, domain);
        return baseAccountMapper.selectOne(queryWrapper);

    }

    @Override
    public BaseAccount registerUsernameAccount(BaseUser baseUser) {
        return this.register(baseUser.getUserId(), baseUser.getUserName(),
                baseUser.getPassword(), AccountType.username.toString(),
                AccountStatus.NORMAL.getKey(), JbmConstants.ACCOUNT_DOMAIN_ADMIN, null);
    }


    /**
     * 注册账号
     *
     * @param userId
     * @param account
     * @param password
     * @param accountType
     * @param status
     * @param domain
     * @param registerIp
     * @return
     */
    @Override
    public BaseAccount register(Long userId, String account, String password, String accountType, Integer status, String domain, String registerIp) {
        BaseAccount baseAccount = this.getAccount(account, accountType, domain);
        if (ObjectUtil.isNotEmpty(baseAccount)) {
            // 账号已被注册
//            throw new RuntimeException(String.format("account=[%s],domain=[%s]", baseAccount.getAccount(), baseAccount.getDomain()));
            return baseAccount;
        }
        //如果没有密码随机注册一个
        if (StrUtil.isEmpty(password)) {
            password = IdUtil.fastSimpleUUID();
        }
        //加密
        String encodePassword = SecurityUtils.encryptPassword(password);
        baseAccount = new BaseAccount(userId, account, encodePassword, accountType, domain, registerIp);
        baseAccount.setCreateTime(new Date());
        baseAccount.setUpdateTime(baseAccount.getCreateTime());
        if (ObjectUtil.isEmpty(status)) {
            status = AccountStatus.NORMAL.getKey();
        }
        baseAccount.setStatus(status);
        baseAccountMapper.insert(baseAccount);
        return baseAccount;
    }


    @Override
    public BaseAccount register(BaseAccount baseAccount) {
        BaseAccount account = this.getAccount(baseAccount.getAccount(), baseAccount.getAccountType(), baseAccount.getDomain());
        if (ObjectUtil.isNotEmpty(account)) {
            // 账号已被注册
//            throw new RuntimeException(String.format("account=[%s],domain=[%s]", baseAccount.getAccount(), baseAccount.getDomain()));
            return account;
        }
        //加密
        String encodePassword = SecurityUtils.encryptPassword(baseAccount.getPassword());
//        BaseAccount baseAccount = new BaseAccount(userId, account, encodePassword, accountType, domain, registerIp);
//        baseAccount.setCreateTime(new Date());
//        baseAccount.setUpdateTime(baseAccount.getCreateTime());
        baseAccount.setStatus(1);
        baseAccountMapper.insert(baseAccount);
        return baseAccount;
    }


    /**
     * 检测账号是否存在
     *
     * @param account
     * @param accountType
     * @param domain
     * @return
     */
    @Override
    public Boolean isExist(String account, String accountType, String domain) {
        QueryWrapper<BaseAccount> queryWrapper = new QueryWrapper();
        queryWrapper.lambda()
                .eq(BaseAccount::getAccount, account)
                .eq(BaseAccount::getAccountType, accountType)
                .eq(BaseAccount::getDomain, domain);
        Long count = baseAccountMapper.selectCount(queryWrapper);
        return count > 0 ? true : false;
    }

    @Override
    public boolean isExist(BaseAccount baseAccount) {
        return this.isExist(baseAccount.getAccount(), baseAccount.getAccountType(), baseAccount.getDomain());
    }


    /**
     * 删除账号
     *
     * @param accountId
     * @return
     */
    @Override
    public int removeAccount(Long accountId) {
        return baseAccountMapper.deleteById(accountId);
    }


    /**
     * 更新账号状态
     *
     * @param accountId
     * @param status
     */
    @Override
    public int updateStatus(Long accountId, Integer status) {
        BaseAccount baseAccount = new BaseAccount();
        baseAccount.setAccountId(accountId);
        baseAccount.setUpdateTime(new Date());
        baseAccount.setStatus(status);
        return baseAccountMapper.updateById(baseAccount);
    }

    /**
     * 根据用户更新账户状态
     *
     * @param userId
     * @param domain
     * @param status
     */
    @Override
    public int updateStatusByUserId(Long userId, String domain, Integer status) {
        if (status == null) {
            return 0;
        }
        BaseAccount baseAccount = new BaseAccount();
        baseAccount.setUpdateTime(new Date());
        baseAccount.setStatus(status);
        QueryWrapper<BaseAccount> wrapper = new QueryWrapper();
        wrapper.lambda()
                .eq(BaseAccount::getDomain, domain)
                .eq(BaseAccount::getUserId, userId);
        return baseAccountMapper.update(baseAccount, wrapper);
    }

    /**
     * 重置用户密码
     *
     * @param userId
     * @param domain
     * @param password
     */
    @Override
    public int updatePasswordByUserId(Long userId, String domain, String password) {
        if (PasswordUtils.checkPassword(password) < 1)
            throw new ServiceException("密码强度不够,请重新设置");
        BaseAccount baseAccount = new BaseAccount();
//        baseAccount.setUpdateTime(new Date());
        baseAccount.setPassword(SecurityUtils.encryptPassword(password));
        QueryWrapper<BaseAccount> wrapper = new QueryWrapper();
        wrapper.lambda()
                .in(BaseAccount::getAccountType, JbmConstants.ACCOUNT_TYPE_USERNAME, JbmConstants.ACCOUNT_TYPE_EMAIL, JbmConstants.ACCOUNT_TYPE_MOBILE)
                .eq(BaseAccount::getUserId, userId)
                .eq(BaseAccount::getDomain, domain);
        return baseAccountMapper.update(baseAccount, wrapper);
    }

    /**
     * 根据用户ID删除账号
     *
     * @param userId
     * @param domain
     * @return
     */
    @Override
    public int removeAccountByUserId(Long userId, String domain) {
        QueryWrapper<BaseAccount> wrapper = new QueryWrapper();
        wrapper.lambda()
                .eq(BaseAccount::getUserId, userId)
                .eq(BaseAccount::getDomain, domain);
        return baseAccountMapper.delete(wrapper);
    }


    /**
     * 添加登录日志
     *
     * @param log
     */
    @Override
    public void addLoginLog(BaseAccountLogs log) {
        QueryWrapper<BaseAccountLogs> queryWrapper = new QueryWrapper();
        queryWrapper.lambda()
//                .eq(BaseAccountLogs::getAccountId, log.getAccountId())
                .eq(BaseAccountLogs::getUserId, log.getUserId());
        Long count = baseAccountLogsMapper.selectCount(queryWrapper);
        log.setLoginTime(DateTime.now());
        log.setLoginNums(count.intValue() + 1);
        baseAccountLogsMapper.insert(log);
    }


}
