package com.jbm.framework.service.mybatis;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.ClassQueryWrapper;
import com.jbm.framework.masterdata.usage.CriteriaQueryWrapper;
import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.masterdata.utils.EntityUtils;
import com.jbm.framework.masterdata.utils.ServiceUtils;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

@Slf4j
public abstract class MasterDataServiceImpl<Entity extends MasterDataEntity> extends BaseServiceImpl<SuperMapper<Entity>, Entity> implements IMasterDataService<Entity> {

    @Autowired
    protected SqlSessionTemplate sqlSessionTemplate;

    @Override
    public Entity selectById(Serializable id) {
        return super.getById(id);
    }

    @Override
    public List<Entity> selectByIds(Collection<? extends Serializable> ids) {
        return Lists.newArrayList(super.listByIds(ids));
    }

    @Override
    public DataPaging<Entity> selectEntitysByWapper(CriteriaQueryWrapper<Entity> criteriaQueryWrapper) {
        IPage pages = this.pageList(criteriaQueryWrapper);
        return ServiceUtils.pageToDataPaging(pages);
    }

    @Override
    public DataPaging<Entity> selectEntitysByWapper(QueryWrapper queryWrapper, PageForm pageForm) {
        final Page<Entity> page = ServiceUtils.buildPage(pageForm);
        final IPage<Entity> data = super.page(page, queryWrapper);
        return ServiceUtils.pageToDataPaging(data, pageForm);
    }

    @Override
    public List<Entity> selectEntitysByWapper(QueryWrapper<Entity> queryWrapper) {
        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public Entity selectEntityByWapper(QueryWrapper<Entity> queryWrapper) {
        return this.getOne(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByWapper(QueryWrapper queryWrapper) {
        return super.remove(queryWrapper);
    }

    @Override
    public Entity selectEntity(Entity entity) {
        return CollectionUtils.firstResult(this.selectEntitys(entity), null);
    }

    @Override
    public Entity selectEntity(Map<String, Object> parameter) {
        return CollectionUtils.firstResult(this.selectEntitys(parameter), null);
    }

    @Override
    public List<Entity> selectEntitys(Entity entity) {
        return selectEntitys(EntityUtils.buildEntityQueryWrapper(entity));
    }

    @Override
    public List<Entity> selectAllEntitys() {
        return super.selectAll();
    }

    @Override
    public List<Entity> selectEntitys(Map<String, Object> params) {
        return this.baseMapper.selectByMap(params);
    }

    @Override
    public DataPaging<Entity> selectEntitys(Entity entity, PageForm pageForm) {
        return this.selectEntitys(ServiceUtils.toCriteriaQueryWrapper(entity, pageForm));
    }

    @Override
    public DataPaging<Entity> selectEntitys(Map<String, Object> params, PageForm pageForm) throws
            DataServiceException {
        return this.selectEntitys(ServiceUtils.toCriteriaQueryWrapper(params, pageForm));
    }

    @Override
    public DataPaging<Entity> selectEntitys(CriteriaQueryWrapper<Entity> wrapper) {
        PageParams pageParams = wrapper.getPageParams();
        IPage list = this.baseMapper.selectPage(pageParams, wrapper);
        //EntityMap.setEnumConvertInterceptor(null);
        return ServiceUtils.pageToDataPaging(list);
    }

    @Override
    public DataPaging<Entity> selectEntitys(PageParams pageParams, QueryWrapper queryWrapper) {
        IPage list = this.baseMapper.selectPage(pageParams, queryWrapper);
        return ServiceUtils.pageToDataPaging(list);
    }

    @Override
    public Map<Serializable, Entity> selectEntityDictionary(Entity entity) {
        List<Entity> list = this.selectEntitys(entity);
        return EntityUtils.entityToDictionary(list);
    }

    @Override
    public Map<Serializable, Entity> selectEntityDictionaryByWapper(CriteriaQueryWrapper criteriaQueryWrapper) {
        List<Entity> list = super.list(criteriaQueryWrapper);
        return EntityUtils.entityToDictionary(list);
    }

    @Override
    public List<Entity> selectEntitys(QueryWrapper queryWrapper) {
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public Entity selectEntity(Entity entity, Entity def) {
        Entity result = this.selectEntity(entity);
        if (result == null) {
            return def;
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Entity insertEntity(Entity entity) {
        super.save(entity);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteEntity(Entity entity) {
        boolean hasId = false;
        try {
            Serializable id = EntityUtils.getKeyValue(entity);
            hasId = ObjectUtil.isNotEmpty(hasId);
            if (hasId) {
                return this.deleteById(id);
            }
        } catch (Exception e) {
            log.error("查找主键错误");
        }
        QueryWrapper wrapper = new QueryWrapper(entity);
        if (wrapper.isEmptyOfWhere()) {
            throw new DataServiceException("没有任何查询添加无法进行删除操作");
        }
        return super.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Serializable id) {
        return super.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Entity saveEntity(Entity entity) {
        if (!super.saveOrUpdate(entity)) {
            log.error("保存失败");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEntity(Entity entity, Entity updateEntity) {
        return super.update(updateEntity, EntityUtils.buildEntityQueryWrapper(updateEntity));
    }

    @Override
    public Long count(Entity entity) {
        return super.count(EntityUtils.buildEntityQueryWrapper(entity));
    }

    @Override
    public Entity selectEntity(Map<String, Object> parameter, Entity def) {
        return ObjectUtil.defaultIfNull(this.selectEntity(parameter), def);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataPaging<Entity> selectEntitys(PageRequestBody pageRequestBody) {
        final Entity entity = pageRequestBody.tryGet(this.currentEntityClass());
        final PageParams pageParams = pageRequestBody.getPageParams();
        IPage<Entity> pages = super.pageList(new CriteriaQueryWrapper(entity, pageParams));
        return ServiceUtils.pageToDataPaging(pages);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean insertEntitys(Collection<Entity> entityList) {
        return super.saveBatch(entityList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveEntitys(Collection<Entity> entityList) {
        return this.saveOrUpdateBatch(entityList, 50);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEntity(Entity entity) {
        return super.updateById(entity);
    }


    // ---------------------------------------------------------直接操作Mapper------------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateByWrapper(Entity entity, Wrapper<Entity> updateWrapper) {
        return super.update(entity, updateWrapper);
    }


    @Override
    public Class<Entity> currentEntityClass() {
        return (Class<Entity>) ReflectionKit.getSuperClassGenericType(getClass(), MasterDataServiceImpl.class, 0);
    }


    /**
     * 泛型方法使用的包装类
     *
     * @return
     */
    protected QueryWrapper currentQueryWrapper() {
        QueryWrapper queryWrapper = ClassQueryWrapper.QueryWrapper(this.currentEntityClass());
        return queryWrapper;
    }

    @Override
    protected Class<Entity> currentModelClass() {
        return (Class<Entity>) ReflectionKit.getSuperClassGenericType(getClass(), BaseServiceImpl.class, 1);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveOrUpdateBatch(Collection<Entity> entityList) {
        return this.saveOrUpdateBatch(entityList, 50);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveOrUpdateBatch(Collection<Entity> entityList, int batchSize) {
        Assert.notEmpty(entityList, "error: entityList must not be empty");
        Class<?> cls = currentModelClass();
        TableInfo tableInfo = TableInfoHelper.getTableInfo(cls);
        Assert.notNull(tableInfo, "error: can not execute. because can not find cache of TableInfo for entity!");
        String keyProperty = tableInfo.getKeyProperty();
        Assert.notEmpty(keyProperty, "error: can not execute. because can not find column for id from entity!");
        int size = entityList.size();
        return super.executeBatch(entityList, batchSize, new BiConsumer<SqlSession, Entity>() {
            @Override
            public void accept(SqlSession sqlSession, Entity entity) {
                Object idVal = ReflectionKit.getFieldValue(entity, keyProperty);
                if (StringUtils.checkValNull(idVal) || Objects.isNull(getById((Serializable) idVal))) {
                    sqlSession.insert(sqlStatement(SqlMethod.INSERT_ONE), entity);
                } else {
                    MapperMethod.ParamMap<Entity> param = new MapperMethod.ParamMap<>();
                    param.put(Constants.ENTITY, entity);
                    sqlSession.update(sqlStatement(SqlMethod.UPDATE_BY_ID), param);
                }
            }
        });
    }

}
