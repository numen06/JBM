package com.jbm.cluster.center.service;

import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.framework.masterdata.service.IMultiPlatformTreeService;

/**
 * @Author: wesley.zhang
 * @Create: 2020-03-24 03:28:09
 */
public interface BaseOrgService extends IMultiPlatformTreeService<BaseOrg> {
    BaseOrg findTopCompany(BaseOrg org);

    BaseOrg findRelegationCompany(BaseOrg org);
}
