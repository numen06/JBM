package com.jbm.framework.service.mybatis;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.service.IBaseSqlService;
import com.jbm.framework.masterdata.usage.bean.BaseEntity;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.ArrayUtils;
import com.jbm.util.CollectionUtils;
import com.jbm.util.MapUtils;
import com.jbm.util.ObjectUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

public class BaseSqlService<Entity extends BaseEntity> extends ServiceImpl<BaseMapper<Entity>, Entity>
        implements IBaseSqlService<Entity>, IService<Entity> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @SuppressWarnings("unchecked")
    @Override
    public Class<Entity> getEntityClass() {
        return currentModelClass();
    }

    public BaseSqlService() {
        super();
    }

    @Autowired(required = false)
    protected SqlSessionTemplate sqlSessionTemplate;

    @Override
    public Entity selectById(Long id) throws DataServiceException {
        return super.getById(id);
    }

    @Override
    public List<Entity> selectByIds(Collection<Long> ids) throws DataServiceException {
        return Lists.newArrayList(super.listByIds(ids));
    }

    @Override
    public DataPaging<Entity> selectEntitysByWapper(QueryWrapper queryWrapper, PageForm pageForm) throws DataServiceException {
        final Page<Entity> page = buildPage(pageForm);
        final IPage<Entity> data = this.baseMapper.selectPage(page, queryWrapper);
        DataPaging<Entity> dataPaging = new DataPaging<>(data.getRecords(), data.getTotal(), data.getPages(), pageForm);
        return dataPaging;
    }

    @Override
    public List<Entity> selectEntitysByWapper(QueryWrapper<Entity> queryWrapper) throws DataServiceException {
        return super.list(queryWrapper);
    }

    @Override
    public Entity selectEntityByWapper(QueryWrapper<Entity> queryWrapper) throws DataServiceException {
        return super.getOne(queryWrapper);
    }

    @Override
    public boolean deleteByWapper(QueryWrapper queryWrapper) throws DataServiceException {
        return super.remove(queryWrapper);
    }

    @Override
    public Entity selectEntity(Entity entity) throws DataServiceException {
        return CollectionUtils.firstResult(this.selectEntitys(entity), null);
    }

    @Override
    public Entity selectEntity(Map<String, Object> parameter) throws DataServiceException {
        return CollectionUtils.firstResult(this.selectEntitys(parameter), null);
    }

    @Override
    public List<Entity> selectEntitys(Entity entity) throws DataServiceException {
        return super.list(buildEntityQueryWrapper(entity));
    }

    @Override
    public List<Entity> selectAllEntitys() throws DataServiceException {
        return super.list(buildEntityQueryWrapper(null));
    }

    @Override
    public List<Entity> selectEntitys(Map<String, Object> params) throws DataServiceException {
        return this.baseMapper.selectByMap(params);
    }

    @Override
    public DataPaging<Entity> selectEntitys(Entity entity, PageForm pageForm) throws DataServiceException {
        final Page<Entity> page = buildPage(pageForm);
        final IPage<Entity> data = this.baseMapper.selectPage(page, this.buildEntityQueryWrapper(entity));
        DataPaging<Entity> dataPaging = new DataPaging<>(data.getRecords(), data.getTotal(), data.getPages(), pageForm);
        return dataPaging;
    }

    @Override
    public DataPaging<Entity> selectEntitys(Map<String, Object> params, PageForm pageForm) throws DataServiceException {
        final Page<Entity> page = buildPage(pageForm);
        final IPage<Entity> data = this.baseMapper.selectPage(page, this.buildMapperQueryWrapper(params));
        DataPaging<Entity> dataPaging = new DataPaging<>(data.getRecords(), data.getTotal(), data.getPages(), pageForm);
        return dataPaging;
    }

    @Override
    public Map<Long, Entity> selectEntityMap(Entity entity) throws DataServiceException {
        List<Entity> list = this.selectEntitys(entity);
        Map<Long, Entity> result = Maps.newHashMap();
        for (Iterator<Entity> iterator = list.iterator(); iterator.hasNext(); ) {
            Entity e = iterator.next();
            result.put(e.getId(), e);
        }
        return result;
    }

    @Override
    public Map<Long, Entity> selectEntityMap(Map<String, Object> parameter) throws DataServiceException {
        List<Entity> list = this.selectEntitys(parameter);
        Map<Long, Entity> result = Maps.newHashMap();
        for (Iterator<Entity> iterator = list.iterator(); iterator.hasNext(); ) {
            Entity e = iterator.next();
            result.put(e.getId(), e);
        }
        return result;
    }

    @Override
    public Entity selectEntity(Entity entity, Entity def) throws DataServiceException {
        Entity result = this.selectEntity(entity);
        if (result == null)
            return def;
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insert(Entity entity) {
        return SqlHelper.retBool(this.baseMapper.insert(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Entity entity) throws DataServiceException {
        return SqlHelper.delBool(this.baseMapper.delete(this.buildEntityQueryWrapper(entity)));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteById(Long id) throws DataServiceException {
        return SqlHelper.delBool(this.baseMapper.deleteById(id));
    }

    @Override
    public Entity saveEntity(Entity entity) throws DataServiceException {
        Long id = entity.getId();
        boolean ret;
        if (id == null) {
            ret = this.insert(entity);
        } else {
            ret = this.update(entity);
        }
        if (!ret) {
            throw new DataServiceException("保存失败");
        }
        return this.selectById(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Entity entity, Entity updateEntity) throws DataServiceException {
        return super.update(updateEntity, this.buildEntityQueryWrapper(updateEntity));
    }

    @Override
    public Integer count(Entity entity) throws DataServiceException {
        return super.count(this.buildEntityQueryWrapper(entity));
    }

    @Override
    public Entity selectEntity(Map<String, Object> parameter, Entity def) throws DataServiceException {
        return ObjectUtils.nullToDefault(this.selectEntity(parameter), def);
    }

//	@Override
//	public List<Entity> selectEntitys(Entity entity, Map<String, Object> expand) throws DataServiceException {
//		return super.list(this.buildMapperQueryWrapper(expand));
//	}

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchById(Collection<Entity> entityList) {
        return super.updateBatchById(entityList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveBatch(Collection<Entity> entityList) {
        return super.saveBatch(entityList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveOrUpdateBatch(Collection<Entity> entityList) {
        return super.saveOrUpdateBatch(entityList, 50);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Entity entity) throws DataServiceException {
        return super.updateById(entity);
    }

    // ---------------------------------------------------------直接操作Mapper------------------------------------------------------------------------

//	protected Entity selectEntity(String statement, Map<String, Object> params) throws DataServiceException {
//		return CollectionUtils.firstResult(this.selectMapperList(statement, params), null);
//	}
//
//	protected List<Entity> selectEntitys(String statement, Map<String, Object> params) throws DataServiceException {
//		return this.selectMapperList(statement, params);
//	}

    protected DataPaging<Entity> selectEntitys(String statement, Map<String, Object> params, PageForm pageForm)
            throws DataServiceException {
        return this.selectMapperPaging(statement, params, pageForm);
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
                                                   PageForm pageForm) throws DataServiceException {
        final Page<Map<String, Object>> page = buildPage(pageForm);
        final Map<String, Object> tempParams = MapUtils.isEmpty(params) ? Maps.newLinkedHashMap() : params;
        tempParams.put(UUID.randomUUID().toString(), page);
        final List<T> list = this.sqlSessionTemplate.selectList(statement, tempParams);
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
    protected <T> T selectMapperOne(final String statement, final Object... args) throws DataServiceException {
        return (T) CollectionUtils.firstResult(selectMapperList(statement, args));
    }

    /**
     * 通过Mapper查询列表
     *
     * @param statement
     * @param args
     * @return
     * @throws DataServiceException
     */
    protected <T> List<T> selectMapperList(final String statement, final Object... args) throws DataServiceException {
        final Object parameter = buildMapperParameter(args);
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
    protected <K, V> Map<K, V> selectMapperMap(final String statement, String mapKey, final Object... args)
            throws DataServiceException {
        final Object parameter = buildMapperParameter(args);
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
    protected int updateMapperMap(final String statement, final Object... args) throws DataServiceException {
        final Object parameter = buildMapperParameter(args);
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
    protected int deleteMapperMap(final String statement, final Object... args) throws DataServiceException {
        final Object parameter = buildMapperParameter(args);
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
    protected int insertMapperMap(final String statement, final Object... args) throws DataServiceException {
        final Object parameter = buildMapperParameter(args);
        return this.sqlSessionTemplate.insert(sqlStatement(statement), parameter);
    }

    // ---------------------------------------------------------build转换方法----------------------------------------------------------------------

    private QueryWrapper<Entity> buildMapperQueryWrapper(Map<String, Object> params) {
        JSONObject json = new JSONObject(params);
        Entity entity = json.toJavaObject(getEntityClass());
        QueryWrapper<Entity> queryWrapper = new QueryWrapper<Entity>(entity);
        for (String key : params.keySet()) {
            Object val = params.get(key);
            if (val instanceof Collection) {
                queryWrapper.in(key, val);
            } else if (val.getClass().isArray()) {
                queryWrapper.in(key, val);
            } else {
                queryWrapper.eq(key, val);
            }
        }
        logger.info("entity:{},params:{}", JSON.toJSONString(entity), JSON.toJSONString(params));
        return queryWrapper;
    }

    private QueryWrapper<Entity> buildEntityQueryWrapper(Entity entity) {
        QueryWrapper<Entity> queryWrapper = new QueryWrapper<Entity>();
        if (entity == null) {
            return queryWrapper;
        }
        if (entity.getId() != null) {
            queryWrapper.eq(TableInfoHelper.getTableInfo(getEntityClass()).getKeyColumn(), entity.getId());
            return queryWrapper;
        }
        queryWrapper = new QueryWrapper<Entity>(entity);
        return queryWrapper;
    }

    private <T> Page<T> buildPage(PageForm pageForm) {
        if (pageForm == null)
            pageForm = PageForm.NO_PAGING();
        final Page<T> page = new Page<T>(pageForm.getCurrPage(), pageForm.getPageSize());
        final Map<String, String> rule = MapUtils.split(pageForm.getSortRule(), Maps.newLinkedHashMap(), ",", ":");
        for (String col : rule.keySet()) {
            String sort = rule.get(col);
            final String unCol = StrUtil.toUnderlineCase(col);
            if ("DESC".equalsIgnoreCase(sort)) {
                page.addOrder(OrderItem.desc(unCol));
            } else {
                page.addOrder(OrderItem.asc(unCol));
            }
        }
        return page;
    }

    private Object buildMapperParameter(final Object[] args) {
        final Map<String, Object> map = Maps.newLinkedHashMap();
        // final RowBounds rowBounds;
        Object parameter = map;
        if (ArrayUtils.getLength(args) == 1)
            parameter = args[0];
        else {
            try {
                parameter = MapUtils.fromArray(args);
            } catch (Exception e) {
                logger.error("组装参数错误", e);
                throw new DataServiceException("组装参数错误", e);
            }
        }
        return parameter;
    }

    /**
     * 拼接mapper全路径
     *
     * @param sqlMethod
     * @return
     */
    protected String sqlStatement(String sqlMethod) {
        if (com.jbm.util.StringUtils.contains(sqlMethod, ".")) {
            return sqlMethod;
        } else {
            return SqlHelper.table(getEntityClass()).getSqlStatement(sqlMethod);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Class<Entity> currentModelClass() {
        return (Class<Entity>) ReflectionKit.getSuperClassGenericType(getClass(), 0);
    }

}
