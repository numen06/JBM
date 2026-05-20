package com.jbm.cluster.common.mysql.service;

import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;

import java.util.List;

public interface BaseUserService extends IMasterDataService<BaseUser> {

    List<BaseUser> selectUserRows(BaseUserForm baseUserForm);

    DataPaging<BaseUser> selectUserRows(BaseUserForm baseUserForm, PageForm pageForm);

    List<BaseUser> findAllList();

    BaseUser getUserById(Long userId);

    BaseUser getUserByPhone(String phone);

    BaseUser getUserByUsername(String username);

    List<BaseUser> getUsersByIds(List<Long> ids);
}