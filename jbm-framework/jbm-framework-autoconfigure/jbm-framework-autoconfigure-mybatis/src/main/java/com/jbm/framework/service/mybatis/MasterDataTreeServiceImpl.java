package com.jbm.framework.service.mybatis;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Maps;
import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.usage.bean.MasterDataTreeEntity;
import com.jbm.util.CollectionUtils;
import com.jbm.util.MapUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MasterDataTreeServiceImpl<Entity extends MasterDataTreeEntity> extends MasterDataServiceImpl<Entity>
        implements IMasterDataTreeService<Entity> {

    @Override
    public List<Entity> selectRootListByCode(Entity entity) throws DataServiceException {
        QueryWrapper<Entity> entityWrapper = new QueryWrapper<>(entity);
        entityWrapper.isNull(StrUtil.toUnderlineCase("parentCode"));
        return super.list(entityWrapper);
    }

    @Override
    public List<Entity> selectRootListByCode() throws DataServiceException {
        QueryWrapper<Entity> entityWrapper = new QueryWrapper<>();
        entityWrapper.isNull(StrUtil.toUnderlineCase("parentCode"));
        return super.list(entityWrapper);
    }

    /**
     * 将列表转换成树列表
     *
     * @param list
     */
    public List<Entity> listToTreeList(List<Entity> list) {
        Map<Long, Entity> tempMap = Maps.newLinkedHashMap();
        //转换成map
        for (Entity entity : list) {
            tempMap.put(entity.getId(), entity);
            entity.setLeaf(true);
        }
        for (Entity entity : list) {
            if (ObjectUtil.isNotEmpty(entity.getParentId())) {
                tempMap.get(entity.getParentId()).setLeaf(false);
            }
        }
        return list;
    }


    public List<Entity> selectChildNodesByCode(Entity entity) throws DataServiceException {
        List<Entity> treeList = new ArrayList<Entity>();
        List<Entity> subEntitys = new ArrayList<Entity>();
        if (entity.getParentCode() == null) {
            subEntitys = this.selectRootListByCode(entity);
        } else {
            subEntitys = this.selectListByParentCode(entity.getCode());
        }
        entity.setLeaf(CollectionUtil.isNotEmpty(subEntitys));
        treeList.addAll(subEntitys);
        treeList.addAll(this.selectChildNodesByCode(subEntitys));
        return treeList;
    }

    public List<Entity> selectChildNodesByCode(List<Entity> subEntitys) throws DataServiceException {
        if (CollectionUtils.isEmpty(subEntitys)) {
            return new ArrayList<Entity>();
        }
        List<Entity> treeList = new ArrayList<Entity>();
        for (Iterator<Entity> iterator = subEntitys.iterator(); iterator.hasNext(); ) {
            Entity subEntity = iterator.next();
            subEntity.setLeaf(true);
            if (ObjectUtil.isEmpty(subEntity.getCode())) {
                continue;
            }
            List<Entity> tempList = this.selectChildNodesByCode(subEntity);
            treeList.addAll(tempList);
        }
        return treeList;
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
    public List<Entity> selectRootListById() throws DataServiceException {
        QueryWrapper<Entity> entityWrapper = new QueryWrapper<>();
        entityWrapper.isNull(StrUtil.toUnderlineCase("parentId"));
        return super.list(entityWrapper);
    }

    @Override
    public List<Entity> selectChildNodesById(Entity entity) throws DataServiceException {
        return this.selectChildNodesById(entity.getId());
    }

    @Override
    public List<Entity> selectChildNodesById(Long parentId) throws DataServiceException {
        List<Entity> treeList = new ArrayList<Entity>();
        List<Entity> subEntitys = new ArrayList<Entity>();
        if (parentId == null) {
            subEntitys = this.selectRootListById();
        } else {
            subEntitys = this.selectListByParentId(parentId);
        }
        treeList.addAll(subEntitys);
        treeList.addAll(this.selectChildNodesById(subEntitys));
        return treeList;
    }


    @Override
    public List<Entity> selectChildNodesByCode(String parentCode) throws DataServiceException {
        List<Entity> treeList = new ArrayList<Entity>();
        List<Entity> subEntitys = new ArrayList<Entity>();
        if (parentCode == null) {
            subEntitys = this.selectRootListByCode();
        } else {
            subEntitys = this.selectListByParentCode(parentCode);
        }
        treeList.addAll(subEntitys);
        treeList.addAll(this.selectChildNodesByCode(subEntitys));
        return treeList;
    }

    @Override
    public List<Entity> selectChildNodesById(List<Entity> subEntitys) throws DataServiceException {
        if (CollectionUtils.isEmpty(subEntitys)) {
            subEntitys = new ArrayList<Entity>();
        }
        List<Entity> treeList = new ArrayList<Entity>();
        for (Iterator<Entity> iterator = subEntitys.iterator(); iterator.hasNext(); ) {
            Entity subEntity = iterator.next();
            subEntity.setLeaf(true);
            if (ObjectUtil.isEmpty(subEntity.getId())) {
                continue;
            }
            List<Entity> tempList = this.selectChildNodesById(subEntity);
            treeList.addAll(tempList);
        }
        return treeList;
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
