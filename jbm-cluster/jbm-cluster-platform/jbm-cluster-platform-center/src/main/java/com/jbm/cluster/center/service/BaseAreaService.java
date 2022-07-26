package com.jbm.cluster.center.service;

import com.jbm.cluster.api.entitys.basic.BaseArea;
import com.jbm.framework.masterdata.service.IMasterDataService;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2022-04-07 21:44:18
 */
public interface BaseAreaService extends IMasterDataService<BaseArea> {
    List<BaseArea> getChinaAreaList();
}
