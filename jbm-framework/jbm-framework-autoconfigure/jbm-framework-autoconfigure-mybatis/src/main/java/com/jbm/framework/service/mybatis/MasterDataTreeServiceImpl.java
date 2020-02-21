package com.jbm.framework.service.mybatis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.usage.entity.MasterDataTreeEntity;
import com.jbm.framework.masterdata.utils.EntityUtils;
import com.jbm.util.CollectionUtils;

public class MasterDataTreeServiceImpl<Entity extends MasterDataTreeEntity> extends MasterDataServiceImpl<Entity> implements IMasterDataTreeService<Entity> {

//    public List<Entity> selectRootListByCode(Entity entity) throws DataServiceException {
//        QueryWrapper<Entity> entityWrapper = new QueryWrapper<>(entity);
//        entityWrapper.isNull(StrUtil.toUnderlineCase("parentCode"));
//        return super.list(entityWrapper);
//    }
//
//    public List<Entity> selectTreeByParentCode(Entity entity) throws DataServiceException {
//        List<Entity> subEntitys = new ArrayList<Entity>();
//        if (entity.getParentCode() == null) {
//            subEntitys = this.selectRootListByCode(entity);
//        } else {
//            subEntitys = this.selectPageList(MapUtils.newParamMap(StrUtil.toUnderlineCase("parentCode"), entity.getCode()));
//        }
//        return this.selectTreeByParentCode(subEntitys);
//    }
//
//    public List<Entity> selectTreeByParentCode(List<Entity> subEntitys) throws DataServiceException {
//        if (CollectionUtils.isEmpty(subEntitys)) {
//            subEntitys = new ArrayList<Entity>();
//        }
//        for (Iterator<Entity> iterator = subEntitys.iterator(); iterator.hasNext(); ) {
//            Entity subEntity = iterator.next();
//            this.selectTreeByParentCode(subEntity);
//        }
//        return subEntitys;
//    }

//    public List<Entity> selectListByParentCode(String parentCode) throws DataServiceException {
//        return this.selectPageList(MapUtils.newParamMap(StrUtil.toUnderlineCase("parentCode"), parentCode));
//    }

    @Override
    public List<Entity> selectRootListById() throws DataServiceException {
        QueryWrapper<Entity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().isNull(Entity::getParentId);
        return super.list(queryWrapper);
    }

    @Override
    public List<Entity> selectTreeByParentId(Entity entity) throws DataServiceException {
        List<Entity> subEntitys = new ArrayList<>();
        if (EntityUtils.keyIsEmpty(entity)) {
            subEntitys = this.selectRootListById();
        } else {
            Long id = EntityUtils.getKeyValue(entity);
            if (id <= 0l) {
                subEntitys = this.selectRootListById();
            } else {
                subEntitys = this.selectListByParentId(entity);
            }
        }
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
        QueryWrapper<Entity> queryWrapper = new QueryWrapper<>();
        if (ObjectUtil.isEmpty(parentId)) {
            queryWrapper.lambda().isNull(Entity::getParentId);
        } else {
            queryWrapper.lambda().eq(Entity::getParentId, parentId);
        }
        return this.selectPageList(queryWrapper);
    }

    @Override
    public List<Entity> selectListByParentId(Entity entity) throws DataServiceException {
        Long id = EntityUtils.getKeyValue(entity);
        return this.selectTreeByParentId(id);
    }


}
