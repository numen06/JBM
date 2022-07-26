package com.jbm.cluster.center.service;

import com.jbm.cluster.api.entitys.basic.BaseUserCertification;
import com.jbm.framework.masterdata.service.IMasterDataService;

/**
 * @Author: wesley.zhang
 * @Create: 2022-07-19 14:01:27
 */
public interface BaseUserCertificationService extends IMasterDataService<BaseUserCertification> {
    BaseUserCertification findByUserId(Long userId);
}
