package com.jbm.cluster.center.service;

import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.framework.masterdata.service.IMultiPlatformTreeService;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2020-03-24 03:28:09
 */
public interface BaseOrgService extends IMultiPlatformTreeService<BaseOrg> {

    /***
     * 获取顶层公司
     * @param org
     * @return
     */
    BaseOrg findTopCompany(BaseOrg org);

    /***
     * 获取下级公司
     * @param org
     * @return
     */
    List<BaseOrg> findRelegationCompany(BaseOrg org);
}
