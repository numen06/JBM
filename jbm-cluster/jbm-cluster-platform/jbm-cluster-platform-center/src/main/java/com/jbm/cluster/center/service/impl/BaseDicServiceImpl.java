package com.jbm.cluster.center.service.impl;

import com.google.common.collect.Maps;
import com.jbm.cluster.api.model.entity.BaseDic;
import com.jbm.cluster.center.service.BaseDicService;
import com.jbm.framework.service.mybatis.MasterDataTreeServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @Author: auto generate by jbm
 * @Create: 2020-02-25 03:47:52
 */
@Service
public class BaseDicServiceImpl extends MasterDataTreeServiceImpl<BaseDic> implements BaseDicService {

    @Override
    @Cacheable(value = "dicMap")
    public Map<String, List<BaseDic>> getDicMap() {
        List<BaseDic> listRoot = this.selectRootListById();
        Map<String, List<BaseDic>> result = Maps.newLinkedHashMap();
        for (int i = 0; i < listRoot.size(); i++) {
            String code = listRoot.get(i).getCode();
            Long id = listRoot.get(i).getId();
            List<BaseDic> listDic = this.selectListByParentId(id);
            result.put(code, listDic);
        }
        return result;
    }

    @Override
    @CacheEvict(value = "dicMap", allEntries = true)
    public BaseDic saveEntity(BaseDic entity) {
        return super.saveEntity(entity);
    }
}
