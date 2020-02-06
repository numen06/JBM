package com.jbm.framework.service.mybatis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Maps;
import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.usage.bean.MasterDataTreeEntity;
import com.jbm.util.CollectionUtils;
import com.jbm.util.MapUtils;

public class MasterDataTreeServiceImpl<Entity extends MasterDataTreeEntity> extends MasterDataServiceImpl<Entity>
        implements IMasterDataTreeService<Entity> {

    public List<Entity> selectRootListByCode(Entity entity) throws DataServiceException {
        QueryWrapper<Entity> entityWrapper = new QueryWrapper<>(entity);
        entityWrapper.isNull(StrUtil.toUnderlineCase("parentCode"));
        return super.list(entityWrapper);
    }

    /**
     * 将列表转换成树列表
     * @param list
     */
    public List<Entity> listToTreeList(List<Entity> list) {
        Map<Long, Entity> tempMap = Maps.newLinkedHashMap();
        //转换成map
        for (Entity entity : list) {
            tempMap.put(entity.getId(), entity);
            entity.setLeaf(false);
        }
        for (Entity entity : list) {
            if (ObjectUtil.isNotEmpty(entity.getParentId())) {
                tempMap.get(entity.getParentId()).setLeaf(true);
            }
        }
        return list;
    }


    public List<Entity> selectTreeByParentCode(Entity entity) throws DataServiceException {
        List<Entity> subEntitys = new ArrayList<Entity>();
        if (entity.getParentCode() == null) {
            subEntitys = this.selectRootListByCode(entity);
        } else {
            subEntitys = this.selectEntitys(MapUtils.newParamMap(StrUtil.toUnderlineCase("parentCode"), entity.getCode()));
        }
        entity.setLeaf(CollectionUtil.isNotEmpty(subEntitys));
        return this.selectTreeByParentCode(subEntitys);
    }

    public List<Entity> selectTreeByParentCode(List<Entity> subEntitys) throws DataServiceException {
        if (CollectionUtils.isEmpty(subEntitys)) {
            subEntitys = new ArrayList<Entity>();
        }
        for (Iterator<Entity> iterator = subEntitys.iterator(); iterator.hasNext(); ) {
            Entity subEntity = iterator.next();
            this.selectTreeByParentCode(subEntity);
        }
        return subEntitys;
    }

    public List<Entity> selectListByParentCode(String parentCode) throws DataServiceException {
        return this.selectEntitys(MapUtils.newParamMap(StrUtil.toUnderlineCase("parentCode"), parentCode));
    }

    @Override
    public List<Entity> selectRootListById(Entity entity) throws DataServiceException {
        QueryWrapper<Entity> entityWrapper = new QueryWrapper<>(entity);
        entityWrapper.isNull(StrUtil.toUnderlineCase("parentId"));
        return super.list(entityWrapper);
    }

    @Override
    public List<Entity> selectTreeByParentId(Entity entity) throws DataServiceException {
        List<Entity> subEntitys = new ArrayList<Entity>();
        if (entity.getId() == null) {
            subEntitys = this.selectRootListById(entity);
        } else {
            subEntitys = this.selectEntitys(MapUtils.newParamMap(StrUtil.toUnderlineCase("parentId"), entity.getId()));
        }
        entity.setLeaf(CollectionUtil.isNotEmpty(subEntitys));
        return this.selectTreeByParentId(subEntitys);
    }

    @Override
    public List<Entity> selectTreeByParentId(Long parentId) throws DataServiceException {
        List<Entity> subEntitys = this.selectListByParentId(parentId);
        return this.selectTreeByParentId(subEntitys);
    }

    @Override
    public List<Entity> selectTreeByParentId(List<Entity> subEntitys) throws DataServiceException {
        if (CollectionUtils.isEmpty(subEntitys)) {
            subEntitys = new ArrayList<Entity>();
        }
        for (Iterator<Entity> iterator = subEntitys.iterator(); iterator.hasNext(); ) {
            Entity subEntity = iterator.next();
            if (ObjectUtil.isEmpty(subEntity.getParentId())) {
                continue;
            }
            this.selectTreeByParentId(subEntity);
        }
        return subEntitys;
    }

    @Override
    public List<Entity> selectListByParentId(Long parentId) throws DataServiceException {
        return this.selectEntitys(MapUtils.newParamMap(StrUtil.toUnderlineCase("parentId"), parentId));
    }

    @Override
    public List<Entity> selectListByParentId(Entity entity) throws DataServiceException {
        return this.selectEntitys(MapUtils.newParamMap(StrUtil.toUnderlineCase("parentId"), entity.getId()));
    }


}
