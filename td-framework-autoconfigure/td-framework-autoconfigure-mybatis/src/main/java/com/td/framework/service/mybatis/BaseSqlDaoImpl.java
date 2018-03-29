package com.td.framework.service.mybatis;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.MybatisMapperAnnotationBuilder;
import com.baomidou.mybatisplus.entity.TableInfo;
import com.baomidou.mybatisplus.enums.SqlMethod;
import com.baomidou.mybatisplus.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.mapper.Condition;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.SqlHelper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.toolkit.StringUtils;
import com.baomidou.mybatisplus.toolkit.TableInfoHelper;
import com.google.common.collect.Maps;
import com.td.framework.dao.mybatis.EntityDaoWarp;
import com.td.framework.dao.mybatis.IBaseSqlDao;
import com.td.framework.dao.mybatis.util.DaoUtils;
import com.td.framework.dao.mybatis.util.JavaStringCompilerUtils;
import com.td.framework.metadata.exceptions.DataServiceException;
import com.td.framework.metadata.usage.bean.PrimaryKey;
import com.td.framework.metadata.usage.page.DataPaging;
import com.td.framework.metadata.usage.page.PageForm;
import com.td.util.ArrayUtils;
import com.td.util.CollectionUtils;
import com.td.util.MapUtils;

public class BaseSqlDaoImpl<Entity extends PrimaryKey<PK>, PK extends Serializable> implements IBaseSqlDao<Entity, PK> {

	protected String sqlStatement(String sqlMethod) {
		return SqlHelper.table(this.entityDaoWarp.getEntityClass()).getSqlStatement(sqlMethod);
	}

	/**
	 * 对Dao层设置的实体封装的一个类
	 */
	private EntityDaoWarp<Entity> entityDaoWarp;

	public EntityDaoWarp<Entity> getEntityDaoWarp() {
		return entityDaoWarp;
	}

	protected final static Logger privateLogger = LoggerFactory.getLogger(BaseSqlDaoImpl.class);

	private final static String MAPPER_TEMP = "package {0};public interface {1} extends com.baomidou.mybatisplus.mapper.BaseMapper<{2}> '{" + "'}";

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired(required = false)
	private SqlSessionTemplate sqlSessionTemplate;
	protected BaseMapper<Entity> baseMapper;

	@Autowired(required = false)
	public void setBaseMapper(BaseMapper<Entity> baseMapper) {
		try {
			this.baseMapper = baseMapper;
			if (baseMapper == null) {
				final Class<?> clazz = JavaStringCompilerUtils.compilerMapper(this.entityDaoWarp.getEntityClass(), MAPPER_TEMP);
				MybatisMapperAnnotationBuilder parser = new MybatisMapperAnnotationBuilder(sqlSessionTemplate.getConfiguration(), clazz);
				parser.parse();
			}
		} catch (Exception e) {
			logger.error("初始化默认mapper出错", e);
		}
	}

	@SuppressWarnings("unchecked")
	public BaseSqlDaoImpl() {
		super();
		if (this instanceof AdvSqlDaoImpl)
			this.entityDaoWarp = DaoUtils.reflectRepositoryBaseDaoWarp(this.getClass());
		else
			this.entityDaoWarp = DaoUtils.reflectEntityBaseDaoWarp(this.getClass());
	}

	// protected String buildDefStatement(String method) {
	// return MessageFormat.format("{0}.{1}", mapperClass, method);
	// }

	@Override
	public boolean deleteByPrimaryKey(PK id) throws DataServiceException {
		return this.deleteById(id);
		// return new
		// Long(sqlSessionTemplate.delete(buildDefStatement("deleteById"), id));
	}

	@Override
	public Entity selectByPrimaryKey(PK id) throws DataServiceException {
		return this.selectById(id);
		// return
		// CollectionUtils.firstResult(sqlSessionTemplate.selectList(buildDefStatement("selectById"),
		// id), null);
	}

	@Override
	public boolean updateByPrimaryKey(Entity entity) throws DataServiceException {
		// EntityWrapper<Entity> wapper = new EntityWrapper<>();
		// wapper.eq("id", entity.getId());
		// Map<String, Object> parameter = new HashMap<String, Object>();
		// parameter.put("ew", wapper);
		// parameter.put("et", entity);
		// return new
		// Long(sqlSessionTemplate.update(buildDefStatement("updateById"),
		// entity));
		return this.updateById(entity);
	}

	@Override
	public Entity selectEntity(Entity entity) throws DataServiceException {
		// SelectMapperParams<Entity> selectMapperParams = new
		// SelectMapperParams<Entity>(entity);
		// return
		// CollectionUtils.firstResult(sqlSessionTemplate.selectList(buildDefStatement("selectList"),
		// selectMapperParams), null);
		return this.selectOnlyEntity(entity);
	}

	@Override
	public <K, V> Entity selectEntity(Map<K, V> parameter) throws DataServiceException {
		// SelectMapperParams<Entity> selectMapperParams = new
		// SelectMapperParams<Entity>(parameter);
		// return
		// CollectionUtils.firstResult(sqlSessionTemplate.selectList(buildDefStatement("selectByMap"),
		// selectMapperParams), null);
		return this.selectOnlyEntity(parameter);
	}

	@Override
	public List<Entity> selectEntitys(Entity entity) throws DataServiceException {
		return this.selectList(new EntityWrapper<Entity>(entity));
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Entity> selectAllEntitys() throws DataServiceException {
		return this.selectList(Condition.EMPTY);
	}

	@Override
	public <K, V> List<Entity> selectEntitys(Map<K, V> params) throws DataServiceException {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(params);
		return sqlSessionTemplate.selectList(sqlStatement("selectByMap"), selectMapperParams);
	}

	@Override
	public DataPaging<Entity> selectEntitys(Entity entity, PageForm pageForm) throws DataServiceException {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(entity, pageForm);
		List<Entity> list = sqlSessionTemplate.selectList(sqlStatement("selectPage"), selectMapperParams, selectMapperParams.getPagination());
		DataPaging<Entity> dataPaging = new DataPaging<Entity>(list, selectMapperParams.getPagination().getTotal(), pageForm.pageable());
		return dataPaging;
	}

	@Override
	public <K, V> DataPaging<Entity> selectEntitys(Map<K, V> params, PageForm pageForm) throws DataServiceException {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(params, pageForm);
		List<Entity> list = sqlSessionTemplate.selectList(sqlStatement("selectPage"), selectMapperParams, selectMapperParams.getPagination());
		DataPaging<Entity> dataPaging = new DataPaging<Entity>(list, selectMapperParams.getPagination().getTotal(), pageForm.pageable());
		return dataPaging;
	}

	@Override
	public DataPaging<Entity> selectEntitys(Entity entity, Map<String, Object> expand, PageForm pageForm) throws DataServiceException {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(entity, pageForm);
		List<Entity> list = sqlSessionTemplate.selectList(sqlStatement("selectPage"), selectMapperParams, selectMapperParams.getPagination());
		DataPaging<Entity> dataPaging = new DataPaging<Entity>(list, selectMapperParams.getPagination().getTotal(), pageForm.pageable());
		return dataPaging;
	}

	@Override
	public boolean delete(Entity entity) throws DataServiceException {
		return this.delete(new EntityWrapper<>(entity));
	}

	@Override
	public Entity save(Entity entity) throws DataServiceException {
		PK id = entity.getId();
		if (id == null) {
			this.insert(entity);
		} else {
			this.update(entity);
		}
		return this.selectByPrimaryKey(entity.getId());
	}

	@Override
	public boolean save(List<Entity> entitys) throws DataServiceException {
		return this.insertBatch(entitys);
	}

	@Override
	public boolean update(Entity entity) throws DataServiceException {
		return this.updateById(entity);
	}

	@Override
	public boolean update(Entity entity, Entity updateEntity) throws DataServiceException {
		return this.update(updateEntity, new EntityWrapper<Entity>(entity));
	}

	@Override
	public Map<PK, Entity> selectEntityMap(Entity entity) throws DataServiceException {
		List<Entity> list = this.selectEntitys(entity);
		Map<PK, Entity> result = Maps.newHashMap();
		for (Iterator<Entity> iterator = list.iterator(); iterator.hasNext();) {
			Entity e = iterator.next();
			result.put(e.getId(), e);
		}
		return result;
	}

	public <K, V> Map<PK, Entity> selectEntityMap(Map<K, V> parameter) throws DataServiceException {
		List<Entity> list = this.selectEntitys(parameter);
		Map<PK, Entity> result = Maps.newHashMap();
		for (Iterator<Entity> iterator = list.iterator(); iterator.hasNext();) {
			Entity e = iterator.next();
			result.put(e.getId(), e);
		}
		return result;
	}

	/**
	 * 将实体转换成为Map以便于对Mapper进行操作
	 * 
	 * @param entity
	 * @return
	 */
	protected Map<String, Object> entityToMap(Entity entity) {
		Map<String, Object> entityMap = MapUtils.newHashMap();
		try {
			entityMap = MapUtils.toMapIgnoreNull(entity);
		} catch (Exception e) {
			logger.error("将实体转换成Map错误", e);
			throw new RuntimeException(e);
		}
		return entityMap;
	}

	@Override
	public <K, V> Entity selectOnlyEntity(Map<K, V> parameter) throws DataServiceException {
		return this.selectEntity(parameter);
	}

	@Override
	public Entity selectOnlyEntity(Entity entity) throws DataServiceException {
		return SqlHelper.getObject(this.selectList(new EntityWrapper<Entity>(entity)));
	}

	@Override
	public <K, V> Entity selectEntity(Map<K, V> parameter, Entity def) throws DataServiceException {
		Entity result = this.selectEntity(parameter);
		if (result == null)
			return def;
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
	public List<Entity> selectEntitys(Entity entity, Map<String, Object> expand) throws DataServiceException {
		Map<String, Object> params = this.entityToMap(entity);
		params.putAll(expand);
		return this.selectEntitys(params);
	}

	@Override
	public <T> List<T> selectMapperList(final String statement, final Object... args) throws DataServiceException {
		final Map<String, Object> map = Maps.newLinkedHashMap();
		// final RowBounds rowBounds;
		Object parameter = map;
		if (ArrayUtils.getLength(args) == 1)
			parameter = args[0];
		else {
			try {
				MapUtils.fromArray(args);
			} catch (Exception e) {
				throw new DataServiceException("组装参数错误", e);
			}
		}
		return this.sqlSessionTemplate.selectList(statement, parameter);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T selectMapperOne(final String statement, final Object... args) throws DataServiceException {
		return (T) CollectionUtils.firstResult(selectMapperList(statement, args));
	}

	@Override
	public <K, V> Map<K, V> selectMapperMap(final String statement, String mapKey, final Object... args) throws DataServiceException {
		final Map<String, Object> map = Maps.newLinkedHashMap();
		// final RowBounds rowBounds;
		Object parameter = map;
		if (ArrayUtils.getLength(args) == 1)
			parameter = args[0];
		else {
			try {
				MapUtils.fromArray(args);
			} catch (Exception e) {
				throw new DataServiceException("组装参数错误", e);
			}
		}
		return this.sqlSessionTemplate.selectMap(statement, parameter, mapKey);
	}

	@Override
	public int updateMapperMap(final String statement, final Object... args) throws DataServiceException {
		final Map<String, Object> map = Maps.newLinkedHashMap();
		// final RowBounds rowBounds;
		Object parameter = map;
		if (ArrayUtils.getLength(args) == 1)
			parameter = args[0];
		else {
			try {
				MapUtils.fromArray(args);
			} catch (Exception e) {
				throw new DataServiceException("组装参数错误", e);
			}
		}
		return this.sqlSessionTemplate.update(statement, parameter);
	}

	@Override
	public int insertMapperMap(final String statement, final Object... args) throws DataServiceException {
		final Map<String, Object> map = Maps.newLinkedHashMap();
		// final RowBounds rowBounds;
		Object parameter = map;
		if (ArrayUtils.getLength(args) == 1)
			parameter = args[0];
		else {
			try {
				MapUtils.fromArray(args);
			} catch (Exception e) {
				throw new DataServiceException("组装参数错误", e);
			}
		}
		return this.sqlSessionTemplate.insert(statement, parameter);
	}

	// **************************************************************

	/**
	 * <p>
	 * 判断数据库操作是否成功
	 * </p>
	 * <p>
	 * 注意！！ 该方法为 Integer 判断，不可传入 int 基本类型
	 * </p>
	 *
	 * @param result
	 *            数据库操作返回影响条数
	 * @return boolean
	 */
	protected static boolean retBool(Integer result) {
		return SqlHelper.retBool(result);
	}

	// @SuppressWarnings("unchecked")
	// protected Class<Entity> currentModelClass() {
	// return ReflectionKit.getSuperClassGenricType(getClass(), 1);
	// }

	/**
	 * <p>
	 * 批量操作 SqlSession
	 * </p>
	 */
	protected SqlSession sqlSessionBatch() {
		return SqlHelper.sqlSessionBatch(this.entityDaoWarp.getEntityClass());
	}

	/**
	 * 获取SqlStatement
	 *
	 * @param sqlMethod
	 * @return
	 */
	protected String sqlStatement(SqlMethod sqlMethod) {
		return SqlHelper.table(this.entityDaoWarp.getEntityClass()).getSqlStatement(sqlMethod.getMethod());
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean insert(Entity entity) {
		return retBool(sqlSessionTemplate.insert(sqlStatement(SqlMethod.INSERT_ONE), entity));
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean insertAllColumn(Entity entity) {
		return retBool(sqlSessionTemplate.insert(sqlStatement(SqlMethod.INSERT_ONE_ALL_COLUMN), entity));
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean insertBatch(List<Entity> entityList) {
		return insertBatch(entityList, 30);
	}

	/**
	 * 批量插入
	 *
	 * @param entityList
	 * @param batchSize
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean insertBatch(List<Entity> entityList, int batchSize) {
		if (CollectionUtils.isEmpty(entityList)) {
			throw new IllegalArgumentException("Error: entityList must not be empty");
		}
		try (SqlSession batchSqlSession = sqlSessionBatch()) {
			int size = entityList.size();
			String sqlStatement = sqlStatement(SqlMethod.INSERT_ONE);
			for (int i = 0; i < size; i++) {
				batchSqlSession.insert(sqlStatement, entityList.get(i));
				if (i >= 1 && i % batchSize == 0) {
					batchSqlSession.flushStatements();
				}
			}
			batchSqlSession.flushStatements();
		} catch (Throwable e) {
			throw new MybatisPlusException("Error: Cannot execute insertBatch Method. Cause", e);
		}
		return true;
	}

	/**
	 * <p>
	 * TableId 注解存在更新记录，否插入一条记录
	 * </p>
	 *
	 * @param entity
	 *            实体对象
	 * @return boolean
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean insertOrUpdate(Entity entity) {
		if (null != entity) {
			Class<?> cls = entity.getClass();
			TableInfo tableInfo = TableInfoHelper.getTableInfo(cls);
			if (null != tableInfo && StringUtils.isNotEmpty(tableInfo.getKeyProperty())) {
				Object idVal = ReflectionKit.getMethodValue(cls, entity, tableInfo.getKeyProperty());
				if (StringUtils.checkValNull(idVal)) {
					return insert(entity);
				} else {
					/*
					 * 更新成功直接返回，失败执行插入逻辑
					 */
					return updateById(entity) || insert(entity);
				}
			} else {
				throw new MybatisPlusException("Error:  Can not execute. Could not find @TableId.");
			}
		}
		return false;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean insertOrUpdateAllColumn(Entity entity) {
		if (null != entity) {
			Class<?> cls = entity.getClass();
			TableInfo tableInfo = TableInfoHelper.getTableInfo(cls);
			if (null != tableInfo && StringUtils.isNotEmpty(tableInfo.getKeyProperty())) {
				Object idVal = ReflectionKit.getMethodValue(cls, entity, tableInfo.getKeyProperty());
				if (StringUtils.checkValNull(idVal)) {
					return insertAllColumn(entity);
				} else {
					/*
					 * 更新成功直接返回，失败执行插入逻辑
					 */
					return updateAllColumnById(entity) || insertAllColumn(entity);
				}
			} else {
				throw new MybatisPlusException("Error:  Can not execute. Could not find @TableId.");
			}
		}
		return false;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean insertOrUpdateBatch(List<Entity> entityList) {
		return insertOrUpdateBatch(entityList, 30);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean insertOrUpdateBatch(List<Entity> entityList, int batchSize) {
		return insertOrUpdateBatch(entityList, batchSize, true);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean insertOrUpdateAllColumnBatch(List<Entity> entityList) {
		return insertOrUpdateBatch(entityList, 30, false);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean insertOrUpdateAllColumnBatch(List<Entity> entityList, int batchSize) {
		return insertOrUpdateBatch(entityList, batchSize, false);
	}

	/**
	 * 批量插入修改
	 *
	 * @param entityList
	 *            实体对象列表
	 * @param batchSize
	 *            批量刷新个数
	 * @param selective
	 *            是否滤掉空字段
	 * @return boolean
	 */
	private boolean insertOrUpdateBatch(List<Entity> entityList, int batchSize, boolean selective) {
		if (CollectionUtils.isEmpty(entityList)) {
			throw new IllegalArgumentException("Error: entityList must not be empty");
		}
		try (SqlSession batchSqlSession = sqlSessionBatch()) {
			int size = entityList.size();
			for (int i = 0; i < size; i++) {
				if (selective) {
					insertOrUpdate(entityList.get(i));
				} else {
					insertOrUpdateAllColumn(entityList.get(i));
				}
				if (i >= 1 && i % batchSize == 0) {
					batchSqlSession.flushStatements();
				}
			}
			batchSqlSession.flushStatements();
		} catch (Throwable e) {
			throw new MybatisPlusException("Error: Cannot execute insertOrUpdateBatch Method. Cause", e);
		}
		return true;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean deleteById(Serializable id) {
		return retBool(sqlSessionTemplate.delete(sqlStatement(SqlMethod.DELETE_BY_ID), id));
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean deleteByMap(Map<String, Object> columnMap) {
		if (MapUtils.isEmpty(columnMap)) {
			throw new MybatisPlusException("deleteByMap columnMap is empty.");
		}
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(columnMap);
		return retBool(sqlSessionTemplate.delete(sqlStatement(SqlMethod.DELETE_BY_MAP), selectMapperParams));
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean delete(Wrapper<Entity> wrapper) {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(wrapper);
		return retBool(sqlSessionTemplate.delete(sqlStatement(SqlMethod.DELETE), selectMapperParams));
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean deleteBatchIds(List<? extends Serializable> idList) {
		return retBool(sqlSessionTemplate.delete(sqlStatement(SqlMethod.DELETE_BATCH_BY_IDS), idList));
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean updateById(Entity entity) {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(entity);
		return retBool(sqlSessionTemplate.update(sqlStatement(SqlMethod.UPDATE_BY_ID), selectMapperParams));
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean updateAllColumnById(Entity entity) {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(entity);
		return retBool(sqlSessionTemplate.update(sqlStatement(SqlMethod.UPDATE_ALL_COLUMN_BY_ID), selectMapperParams));
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean update(Entity entity, Wrapper<Entity> wrapper) {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(entity, wrapper);
		return retBool(sqlSessionTemplate.update(sqlStatement(SqlMethod.UPDATE), selectMapperParams));
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean updateBatchById(List<Entity> entityList) {
		return updateBatchById(entityList, 30);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean updateBatchById(List<Entity> entityList, int batchSize) {
		return updateBatchById(entityList, batchSize, true);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean updateAllColumnBatchById(List<Entity> entityList) {
		return updateAllColumnBatchById(entityList, 30);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean updateAllColumnBatchById(List<Entity> entityList, int batchSize) {
		return updateBatchById(entityList, batchSize, false);
	}

	/**
	 * 根据主键ID进行批量修改
	 *
	 * @param entityList
	 *            实体对象列表
	 * @param batchSize
	 *            批量刷新个数
	 * @param selective
	 *            是否滤掉空字段
	 * @return boolean
	 */
	private boolean updateBatchById(List<Entity> entityList, int batchSize, boolean selective) {
		if (CollectionUtils.isEmpty(entityList)) {
			throw new IllegalArgumentException("Error: entityList must not be empty");
		}
		try (SqlSession batchSqlSession = sqlSessionBatch()) {
			int size = entityList.size();
			SqlMethod sqlMethod = selective ? SqlMethod.UPDATE_BY_ID : SqlMethod.UPDATE_ALL_COLUMN_BY_ID;
			String sqlStatement = sqlStatement(sqlMethod);
			for (int i = 0; i < size; i++) {
				MapperMethod.ParamMap<Entity> param = new MapperMethod.ParamMap<>();
				param.put("et", entityList.get(i));
				batchSqlSession.update(sqlStatement, param);
				if (i >= 1 && i % batchSize == 0) {
					batchSqlSession.flushStatements();
				}
			}
			batchSqlSession.flushStatements();
		} catch (Throwable e) {
			throw new MybatisPlusException("Error: Cannot execute updateBatchById Method. Cause", e);
		}
		return true;
	}

	@Override
	public Entity selectById(Serializable id) {
		return sqlSessionTemplate.selectOne(sqlStatement(SqlMethod.SELECT_BY_ID), id);
		// return baseMapper.selectById(id);
	}

	@Override
	public List<Entity> selectBatchIds(List<? extends Serializable> idList) {
		return sqlSessionTemplate.selectList(sqlStatement(SqlMethod.SELECT_BATCH_BY_IDS), idList);
	}

	@Override
	public List<Entity> selectByMap(Map<String, Object> columnMap) {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(columnMap);
		return sqlSessionTemplate.selectList(sqlStatement(SqlMethod.SELECT_BY_MAP), selectMapperParams);
	}

	@Override
	public Entity selectOne(Wrapper<Entity> wrapper) {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(wrapper);
		return SqlHelper.getObject(sqlSessionTemplate.selectList(sqlStatement(SqlMethod.SELECT_ONE), selectMapperParams));
	}

	@Override
	public Map<String, Object> selectMap(Wrapper<Entity> wrapper) {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(wrapper);
		return SqlHelper.getObject(sqlSessionTemplate.selectList(sqlStatement(SqlMethod.SELECT_BY_MAP), selectMapperParams));
	}

	@Override
	public Object selectObj(Wrapper<Entity> wrapper) {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(wrapper);
		return SqlHelper.getObject(sqlSessionTemplate.selectList(sqlStatement(SqlMethod.SELECT_OBJS), selectMapperParams));
	}

	@Override
	public int selectCount(Wrapper<Entity> wrapper) {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(wrapper);
		return SqlHelper.retCount(sqlSessionTemplate.selectOne(sqlStatement(SqlMethod.SELECT_COUNT), selectMapperParams));
	}

	@Override
	public List<Entity> selectList(Wrapper<Entity> wrapper) {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(wrapper);
		return sqlSessionTemplate.selectList(sqlStatement(SqlMethod.SELECT_LIST), selectMapperParams);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Page<Entity> selectPage(Page<Entity> page) {
		return selectPage(page, Condition.EMPTY);
	}

	@Override
	public List<Map<String, Object>> selectMaps(Wrapper<Entity> wrapper) {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(wrapper);
		return sqlSessionTemplate.selectList(sqlStatement(SqlMethod.SELECT_MAPS), selectMapperParams);
	}

	@Override
	public List<Object> selectObjs(Wrapper<Entity> wrapper) {
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(wrapper);
		return sqlSessionTemplate.selectList(sqlStatement(SqlMethod.SELECT_OBJS), selectMapperParams);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Page<Map<String, Object>> selectMapsPage(Page page, Wrapper<Entity> wrapper) {
		SqlHelper.fillWrapper(page, wrapper);
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(page, wrapper);
		page.setRecords(sqlSessionTemplate.selectList(sqlStatement(SqlMethod.SELECT_MAPS_PAGE), selectMapperParams));
		// page.setRecords(baseMapper.selectMapsPage(page, wrapper));
		return page;
	}

	@Override
	public Page<Entity> selectPage(Page<Entity> page, Wrapper<Entity> wrapper) {
		SqlHelper.fillWrapper(page, wrapper);
		SelectMapperParams<Entity> selectMapperParams = new SelectMapperParams<Entity>(page, wrapper);
		page.setRecords(sqlSessionTemplate.selectList(sqlStatement(SqlMethod.SELECT_PAGE), selectMapperParams));
		// page.setRecords(baseMapper.selectPage(page, wrapper));
		return page;
	}
}
