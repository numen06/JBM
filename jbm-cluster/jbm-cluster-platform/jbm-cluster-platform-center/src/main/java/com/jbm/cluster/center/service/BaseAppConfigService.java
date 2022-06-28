package com.jbm.cluster.center.service;

import com.jbm.cluster.api.entitys.basic.BaseAppConfig;
import com.jbm.framework.masterdata.service.IMasterDataService;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-06-27 12:55:11
 */
public interface BaseAppConfigService extends IMasterDataService<BaseAppConfig> {
    BaseAppConfig getAppConfigByKey(String appKey);
}
