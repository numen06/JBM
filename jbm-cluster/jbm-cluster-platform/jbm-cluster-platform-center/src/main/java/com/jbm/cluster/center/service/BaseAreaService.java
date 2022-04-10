package com.jbm.cluster.center.service;

import com.jbm.cluster.api.model.entity.BaseArea;
import com.jbm.cluster.center.service.impl.BaseAreaServiceImpl;
import com.jbm.framework.masterdata.service.IMasterDataService;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Map;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-04-07 21:44:18
 */
public interface BaseAreaService extends IMasterDataService<BaseArea> {
    Map<String, List<BaseArea>> getChinaAreaMap();
}
