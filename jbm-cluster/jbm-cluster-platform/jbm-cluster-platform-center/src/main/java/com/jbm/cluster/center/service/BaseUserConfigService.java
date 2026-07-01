package com.jbm.cluster.center.service;

import com.jbm.cluster.api.entitys.basic.BaseUserConfig;
import com.jbm.framework.masterdata.service.IMasterDataService;

/**
 * @Author: wesley.zhang
 * @Create: 2021-08-25 11:19:05
 */
public interface BaseUserConfigService extends IMasterDataService<BaseUserConfig> {

    /**
     * 按用户与应用查询配置；存在多条历史重复数据时返回最近更新的一条。
     */
    BaseUserConfig findByUserIdAndAppId(Long userId, Long appId);
}
