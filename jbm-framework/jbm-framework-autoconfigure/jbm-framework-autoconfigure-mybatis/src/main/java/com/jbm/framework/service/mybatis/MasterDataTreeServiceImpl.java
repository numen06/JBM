package com.jbm.framework.service.mybatis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.usage.bean.MasterDataTreeEntity;
import com.jbm.util.CollectionUtils;
import com.jbm.util.MapUtils;

public class MasterDataTreeServiceImpl<Entity extends MasterDataTreeEntity> extends MasterDataServiceImpl<Entity>
        implements IMasterDataTreeService<Entity> {

    public List<Entity> selectRootListByCode(Entity entity) throws DataServiceException {
        QueryWrapper<Entity> entityWrapper = new QueryWrapper<>(entity);
        entityWrapper.isNull("parent_code");
        return super.list(entityWrapper);
    }

    public List<Entity> selectTreeByParentCode(Entity entity) throws DataServiceException {
        String parentCode = entity.getParentCode();
        List<Entity> subEntitys = new ArrayList<Entity>();
        if (parentCode == null) {
            subEntitys = this.selectRootListByCode(entity);
        } else {
            subEntitys = this.selectEntitys(MapUtils.newParamMap("parentCode", entity.getParentCode()));
        }
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
        return this.selectEntitys(MapUtils.newParamMap("parentCode", parentCode));
    }

    @Override
    public List<Entity> selectRootListById(Entity entity) throws DataServiceException {
        QueryWrapper<Entity> entityWrapper = new QueryWrapper<>(entity);
        entityWrapper.isNull("id");
        return super.list(entityWrapper);
    }

    @Override
    public List<Entity> selectTreeByParentId(Entity entity) throws DataServiceException {
        Long parentId = entity.getParentId();
        List<Entity> subEntitys = new ArrayList<Entity>();
        if (parentId == null) {
            subEntitys = this.selectRootListById(entity);
        } else {
            subEntitys = this.selectEntitys(MapUtils.newParamMap("parentId", entity.getParentCode()));
        }
        return this.selectTreeByParentCode(subEntitys);
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
            this.selectTreeByParentId(subEntity);
        }
        return subEntitys;
    }

    @Override
    public List<Entity> selectListByParentId(Long parentId) throws DataServiceException {
        return this.selectEntitys(MapUtils.newParamMap("parentId", parentId));
    }

    @Override
    public List<Entity> selectListByParentId(Entity entity) throws DataServiceException {
        return this.selectEntitys(MapUtils.newParamMap("parentId", entity.getId()));
    }


}
