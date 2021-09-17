package com.jbm.framework.service.mybatis;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.service.IMultiPlatformTreeService;
import com.jbm.framework.masterdata.usage.entity.MultiPlatformTreeEntity;
import com.jbm.framework.masterdata.utils.EntityUtils;
import com.jbm.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
public abstract class MultiPlatformTreeServiceImpl<Entity extends MultiPlatformTreeEntity> extends MasterDataServiceImpl<Entity> implements IMultiPlatformTreeService<Entity> {


    @Override
    public List<Entity> selectRootListById(Entity entity) throws DataServiceException {
        QueryWrapper<Entity> queryWrapper = currentQueryWrapper();
        queryWrapper.isNull(EntityUtils.toDbName(Entity::getParentId));
        return this.selectEntitysByWapper(queryWrapper);
    }

    @Override
    public List<Entity> selectChildNodesById(Entity entity) throws DataServiceException {
        List<Entity> subEntitys = this.selectChildNodesById(entity.getId());
        entity.setLeaf(CollectionUtil.isNotEmpty(subEntitys) ? false : true);
        return subEntitys;
    }

    @Override
    public List<Entity> selectChildNodesById(Long parentId) throws DataServiceException {
        List<Entity> treeList = new ArrayList<Entity>();
        List<Entity> subEntitys = new ArrayList<Entity>();
        if (parentId == null) {
            subEntitys = this.selectRootListById(null);
        } else {
            subEntitys = this.selectListByParentId(parentId);
        }
        treeList.addAll(subEntitys);
        //递归查询下一层
        treeList.addAll(this.selectChildNodesById(subEntitys));
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
        QueryWrapper<Entity> queryWrapper = currentQueryWrapper();
        queryWrapper.eq(EntityUtils.toDbName(Entity::getParentId), parentId);
        return this.selectEntitysByWapper(queryWrapper);
    }

    @Override
    public List<Entity> selectListByParentId(Entity entity) throws DataServiceException {
        QueryWrapper<Entity> queryWrapper = currentQueryWrapper();
        queryWrapper.eq(EntityUtils.toDbName(Entity::getParentId), entity.getId());
        return this.selectEntitysByWapper(queryWrapper);
    }
}
