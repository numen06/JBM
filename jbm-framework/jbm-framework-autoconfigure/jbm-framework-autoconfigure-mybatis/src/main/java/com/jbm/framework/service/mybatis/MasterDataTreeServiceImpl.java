package com.jbm.framework.service.mybatis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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

    public List<Entity> selectTreeByParentCode(Entity entity) throws DataServiceException {
        List<Entity> subEntitys = new ArrayList<Entity>();
        if (entity.getParentCode() == null) {
            subEntitys = this.selectRootListByCode(entity);
        } else {
            subEntitys = this.selectEntitys(MapUtils.newParamMap(StrUtil.toUnderlineCase("parentCode"), entity.getCode()));
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
        return this.selectEntitys(MapUtils.newParamMap(StrUtil.toUnderlineCase("parentId"), parentId));
    }

    @Override
    public List<Entity> selectListByParentId(Entity entity) throws DataServiceException {
        return this.selectEntitys(MapUtils.newParamMap(StrUtil.toUnderlineCase("parentId"), entity.getId()));
    }


}
