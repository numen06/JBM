package com.jbm.cluster.center.service;

import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2020-03-24 03:28:09
 */
public interface BaseOrgService extends IMasterDataTreeService<BaseOrg> {

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

    /**
     * 根据公司编码查询
     *
     * @param baseOrg
     * @return
     */
    BaseOrg getBaseOrg(BaseOrg baseOrg);
}
