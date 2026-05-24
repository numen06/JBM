package com.jbm.cluster.common.mysql.service;

import com.jbm.cluster.api.entitys.basic.BaseUserOrg;
import com.jbm.framework.masterdata.service.IMasterDataService;

import java.util.List;

public interface BaseUserOrgService extends IMasterDataService<BaseUserOrg> {

    List<BaseUserOrg> findUserOrgs(Long userId);

    List<Long> getActiveOrgIds(Long userId);

    void saveUserOrgs(Long userId, String... orgIds);

    void removeUserOrgs(Long userId);
}
