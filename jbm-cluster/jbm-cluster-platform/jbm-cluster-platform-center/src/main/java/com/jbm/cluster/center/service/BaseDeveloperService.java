package com.jbm.cluster.center.service;

import com.jbm.cluster.api.model.UserAccount;
import com.jbm.cluster.api.model.entity.BaseDeveloper;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.usage.paging.DataPaging;

import java.util.List;

/**
 * 系统用户资料管理
 *
 * @author: wesley.zhang
 * @date: 2018/10/24 16:38
 * @description:
 */
public interface BaseDeveloperService extends IMasterDataService<BaseDeveloper> {

    /**
     * 添加用户信息
     * @param baseDeveloper
     * @return
     */
    void addUser(BaseDeveloper baseDeveloper);

    /**
     * 更新系统用户
     *
     * @param baseDeveloper
     * @return
     */
    void updateUser(BaseDeveloper baseDeveloper);

    /**
     * 添加第三方登录用户
     *
     * @param baseDeveloper
     * @param accountType
     * @param
     */
    void addUserThirdParty(BaseDeveloper baseDeveloper, String accountType);

    /**
     * 更新密码
     *
     * @param userId
     * @param password
     */
    void updatePassword(Long userId, String password);


    DataPaging<BaseDeveloper> findListPage(PageRequestBody pageRequestBody);

    /**
     * 查询列表
     *
     * @return
     */
    List<BaseDeveloper> findAllList();


    /**
     * 根据用户ID获取用户信息
     *
     * @param userId
     * @return
     */
    BaseDeveloper getUserById(Long userId);

    /**
     * 依据登录名查询系统用户信息
     *
     * @param username
     * @return
     */
    BaseDeveloper getUserByUsername(String username);


    /**
     * 支持密码、手机号、email登陆
     * 其他方式没有规则，无法自动识别。需要单独开发
     *
     * @param account 登陆账号
     * @return
     */
    UserAccount login(String account);
}
