package com.jbm.cluster.center.business;

import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.cluster.api.form.ThirdPartyUserForm;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.common.mysql.service.BaseUserService;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;

import java.util.List;

public interface BaseUserBusiness extends BaseUserService {

    List<BaseUser> selectEntitys(BaseUserForm baseUserForm);

    DataPaging<BaseUser> selectEntitys(BaseUserForm baseUserForm, PageForm pageForm);

    void register(BaseUser baseUser, String registerIp);

    Boolean close(BaseUser baseUser);

    void addUser(BaseUser baseUser);

    void activationEmailAccount(BaseUser baseUser);

    void activationMobileAccount(BaseUser baseUser);

    void updateUser(BaseUser baseUser);

    void addUserThirdParty(BaseUser baseUser, String accountType);

    void bindUserThirdPartyByPhone(String phone, BaseAccount baseAccount);

    void updatePassword(Long userId, String password);

    DataPaging<BaseUser> findListPage(BaseUserForm form);

    UserAccount getUserAccount(Long userId);

    UserAccount login(String account);

    UserAccount login(String account, String loginType);

    List<BaseUser> retrievalUsers(String keyword);

    UserAccount registerAccountByPhone(String phone, String username, String password, String accountType);

    UserAccount loginAndRegisterMobileUser(ThirdPartyUserForm thirdPartyUserForm);

    List<BaseRole> getUserRoles(Long userId);

    List<Long> getUserRoleIds(Long userId);
}