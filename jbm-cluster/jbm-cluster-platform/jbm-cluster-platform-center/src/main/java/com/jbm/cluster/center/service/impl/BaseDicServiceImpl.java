package com.jbm.cluster.center.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Maps;
import com.jbm.cluster.api.model.entity.BaseDic;
import com.jbm.cluster.center.service.BaseDicService;
import com.jbm.framework.masterdata.utils.EntityUtils;
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
    private final static String CACHE_KEY = "areaMap";

    @Override
    @Cacheable(value = CACHE_KEY)
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
    @CacheEvict(value = CACHE_KEY, allEntries = true)
    public BaseDic saveEntity(BaseDic entity) {
        return super.saveEntity(entity);
    }

    @Override
    public BaseDic getBaseDicType(String code) {
        QueryWrapper<BaseDic> queryWrapper = currentQueryWrapper();
        queryWrapper.isNull(EntityUtils.toDbName(BaseDic::getParentId));
        queryWrapper.eq(EntityUtils.toDbName(BaseDic::getCode), code);
        return this.selectEntityByWapper(queryWrapper);
    }

    @Override
    public BaseDic getBaseDic(Long parentId, String code) {
        QueryWrapper<BaseDic> queryWrapper = currentQueryWrapper();
        queryWrapper.eq(EntityUtils.toDbName(BaseDic::getParentId), parentId);
        queryWrapper.eq(EntityUtils.toDbName(BaseDic::getCode), code);
        return this.selectEntityByWapper(queryWrapper);
    }

}
