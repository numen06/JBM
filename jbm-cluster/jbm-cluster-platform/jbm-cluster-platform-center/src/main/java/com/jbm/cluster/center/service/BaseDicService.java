package com.jbm.cluster.center.service;

import com.jbm.cluster.api.entitys.basic.BaseDic;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;

import java.util.List;
import java.util.Map;

/**
 * @Author: wesley.zhang
 * @Create: 2020-02-25 03:47:52
 */
public interface BaseDicService extends IMasterDataTreeService<BaseDic> {
    Map<String, List<BaseDic>> getDicMap();

    BaseDic getBaseDicType(String code);

    BaseDic getBaseDic(Long parentId, String code);
}
