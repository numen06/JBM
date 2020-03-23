package com.jbm.framework.service.mybatis;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import com.jbm.util.MapUtils;
import com.jbm.util.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public abstract class MasterDataServiceImpl<Entity extends MasterDataEntity> extends BaseServiceImpl<SuperMapper<Entity>, Entity> implements IMasterDataService<Entity> {

    @Override
    public Entity selectById(Long id) {
        return super.getById(id);
    }

    @Override
    public List<Entity> selectByIds(Collection<Long> ids) {
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
    public Map<Long, Entity> selectEntityDictionary(Entity entity) {
        List<Entity> list = this.selectEntitys(entity);
        return EntityUtils.entityToDictionary(list);
    }

    @Override
    public Map<Long, Entity> selectEntityDictionaryByWapper(CriteriaQueryWrapper criteriaQueryWrapper) {
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
        if (result == null)
            return def;
        return result;
    }

    @Override
    public Entity insertEntity(Entity entity) {
        super.save(entity);
        return entity;
    }

    @Override
    public boolean deleteEntity(Entity entity) {
        boolean hasId = false;
        try {
            Long id = EntityUtils.getKeyValue(entity);
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
    public boolean deleteById(Long id) {
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
    public Integer count(Entity entity) {
        return super.count(EntityUtils.buildEntityQueryWrapper(entity));
    }

    @Override
    public Entity selectEntity(Map<String, Object> parameter, Entity def) {
        return ObjectUtils.nullToDefault(this.selectEntity(parameter), def);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataPaging<Entity> selectEntitys(PageRequestBody<Entity> pageRequestBody) {
        final Entity entity = pageRequestBody.tryGet(this.currentEntityClass());
        final PageParams pageParams = pageRequestBody.getPageParams();
        IPage<Entity> pages = super.pageList(new CriteriaQueryWrapper(entity, pageParams));
        return ServiceUtils.pageToDataPaging(pages);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchById(Collection<Entity> entityList) {
        return super.updateBatchById(entityList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean insertEntitys(Collection<Entity> entityList) {
        return super.saveBatch(entityList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveEntitys(Collection<Entity> entityList) {
        return super.saveOrUpdateBatch(entityList, 50);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEntity(Entity entity) {
        return super.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateByWrapper(Entity entity, Wrapper<Entity> updateWrapper) {
        return super.update(entity, updateWrapper);
    }


    // ---------------------------------------------------------直接操作Mapper------------------------------------------------------------------------

    @Autowired
    protected SqlSessionTemplate sqlSessionTemplate;

    protected DataPaging<Entity> selectEntitys(String statement, Map<String, Object> params, PageForm pageForm) {
        return this.selectMapperPaging(sqlStatement(statement), params, pageForm);
    }

    /**
     * 通过Mapper查询分页
     *
     * @param statement
     * @param params
     * @param pageForm
     * @return
     * @throws DataServiceException
     */
    protected <T> DataPaging<T> selectMapperPaging(final String statement, Map<String, Object> params,
                                                   PageForm pageForm) {
        final Page<Map<String, Object>> page = ServiceUtils.buildPage(pageForm);
        final Map<String, Object> tempParams = MapUtils.isEmpty(params) ? Maps.newLinkedHashMap() : params;
        tempParams.put(UUID.randomUUID().toString(), page);
        final List<T> list = this.sqlSessionTemplate.selectList(sqlStatement(statement), tempParams);
        final DataPaging<T> dataPaging = new DataPaging<>(list, page.getTotal(), page.getPages(), pageForm);
        return dataPaging;
    }

    /**
     * 通过Mapper查询一个实体
     *
     * @param statement
     * @param args
     * @return
     * @throws DataServiceException
     */
    @SuppressWarnings("unchecked")
    protected <T> T selectMapperOne(final String statement, final Object... args) {
        return (T) CollectionUtils.firstResult(selectMapperList(sqlStatement(statement), args));
    }


    /**
     * 通过Mapper查询列表
     *
     * @param statement
     * @param args
     * @return
     * @throws DataServiceException
     */
    protected <T> List<T> selectMapperList(final String statement, final Object... args) throws
            DataServiceException {
        final Object parameter = ServiceUtils.buildMapperParameter(args);
        return this.sqlSessionTemplate.selectList(sqlStatement(statement), parameter);
    }

    /**
     * 通过Mapper查询Map
     *
     * @param statement
     * @param mapKey
     * @param args
     * @return
     * @throws DataServiceException
     */
    protected <K, V> Map<K, V> selectMapperMap(final String statement, String mapKey, final Object... args) {
        final Object parameter = ServiceUtils.buildMapperParameter(args);
        return this.sqlSessionTemplate.selectMap(sqlStatement(statement), parameter, mapKey);
    }

    /**
     * 通过Mapper更新数据
     *
     * @param statement
     * @param args
     * @return
     * @throws DataServiceException
     */
    protected int updateMapperMap(final String statement, final Object... args) {
        final Object parameter = ServiceUtils.buildMapperParameter(args);
        return this.sqlSessionTemplate.update(sqlStatement(statement), parameter);
    }

    /**
     * 通过Mapper删除数据
     *
     * @param statement
     * @param args
     * @return
     * @throws DataServiceException
     */
    @Transactional(rollbackFor = Exception.class)
    protected int deleteMapperMap(final String statement, final Object... args) {
        final Object parameter = ServiceUtils.buildMapperParameter(args);
        return this.sqlSessionTemplate.delete(sqlStatement(statement), parameter);
    }

    /**
     * 通过Mapper插入数据
     *
     * @param statement
     * @param args
     * @return
     * @throws DataServiceException
     */
    @Transactional(rollbackFor = Exception.class)
    protected int insertMapperMap(final String statement, final Object... args) {
        final Object parameter = ServiceUtils.buildMapperParameter(args);
        return this.sqlSessionTemplate.insert(sqlStatement(statement), parameter);
    }


    protected String sqlStatement(String statement) {
        return ServiceUtils.sqlStatement(this.currentEntityClass(), statement);
    }

    @Override
    public Class<Entity> currentEntityClass() {
        return (Class<Entity>) ReflectionKit.getSuperClassGenericType(getClass(), 0);
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


}
