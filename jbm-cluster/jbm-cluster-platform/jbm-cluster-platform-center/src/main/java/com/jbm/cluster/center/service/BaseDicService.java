package com.jbm.cluster.center.service;

import com.jbm.cluster.api.model.entity.BaseDic;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;

import java.util.List;
import java.util.Map;

/**
 * @Author: auto generate by jbm
 * @Create: 2020-02-25 03:47:52
 */
public interface BaseDicService extends IMasterDataTreeService<BaseDic> {
    Map<String, List<BaseDic>> getDicMap();
}
