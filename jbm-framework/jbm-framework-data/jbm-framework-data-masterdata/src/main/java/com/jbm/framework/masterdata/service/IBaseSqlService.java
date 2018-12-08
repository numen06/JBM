package com.jbm.framework.masterdata.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.usage.bean.BaseEntity;
import com.jbm.framework.masterdata.usage.paging.DataPaging;
import com.jbm.framework.masterdata.usage.paging.PageForm;

public interface IBaseSqlService<Entity extends BaseEntity> {

	public Class<Entity> getEntityClass();

	/**
	 * 保存（持久化）对象
	 * 
	 * @return 执行成功数
	 */
	public Entity saveEntity(Entity entity) throws DataServiceException;

	/**
	 * 删除（持久化）对象
	 * 
	 * @param entity
	 * @return
	 */
	public boolean delete(Entity entity) throws DataServiceException;

	/**
	 * 更新（持久化）对象
	 * 
	 * @param entity 持久化对象
	 * @return 执行成功的记录个数
	 */
	public boolean update(Entity entity) throws DataServiceException;

	/**
	 * 通过实体去查询其他信息一般用于ID
	 * 
	 * @param entity
	 * @return
	 */
	public Entity selectEntity(Entity entity) throws DataServiceException;

	/**
	 * 不分页查询
	 * 
	 * @param entity
	 * @return 查询结果
	 */
	public List<Entity> selectEntitys(Entity entity) throws DataServiceException;

	/**
	 * 查询实体
	 * 
	 * @param entity
	 * @param pageable
	 * @return
	 */
	public DataPaging<Entity> selectEntitys(Entity entity, PageForm pageForm) throws DataServiceException;

	/**
	 * 统计数量
	 * 
	 * @param entity
	 * @return
	 */
	Integer count(Entity entity) throws DataServiceException;

	/**
	 * 查询实体
	 * 
	 * @param entity
	 * @param def
	 * @return
	 */
	Entity selectEntity(Entity entity, Entity def) throws DataServiceException;

//	/**
//	 * 通过实体查询分页，并且加入扩展字段
//	 * 
//	 * @param entity   实体
//	 * @param expand   扩展字段
//	 * @param pageForm 分页信息
//	 * @return
//	 */
//	DataPaging<Entity> selectEntitys(Entity entity, Map<String, Object> expand, PageForm pageForm)
//			throws DataServiceException;

//	/**
//	 * 通过实体查询列表，并且加入扩展
//	 * 
//	 * @param entity 实体
//	 * @param expand 扩展
//	 * @return
//	 */
//	List<Entity> selectEntitys(Entity entity, Map<String, Object> expand) throws DataServiceException;

	/**
	 * 查询所有实体
	 * 
	 * @return
	 */
	List<Entity> selectAllEntitys() throws DataServiceException;

//	<T> List<T> selectMapperList(String statement, Object... args) throws DataServiceException;

//	<T> T selectMapperOne(String statement, Object... args) throws DataServiceException;

//	<K, V> Map<K, V> selectMapperMap(String statement, String mapKey, Object... args) throws DataServiceException;
//
//	int updateMapperMap(String statement, Object... args) throws DataServiceException;
//
//	int insertMapperMap(String statement, Object... args) throws DataServiceException;

	boolean update(Entity entity, Entity updateEntity) throws DataServiceException;

//	List<Entity> selectList(Wrapper<Entity> wrapper);

	Entity selectById(Long id) throws DataServiceException;

	List<Entity> selectEntitys(Map<String, Object> params) throws DataServiceException;

	DataPaging<Entity> selectEntitys(Map<String, Object> params, PageForm pageForm) throws DataServiceException;

	/**
	 * 查找列表，将实体的主键作为KEY输出为MAP
	 * 
	 * @param entity
	 * @return
	 * @throws DataServiceException
	 */
	Map<Long, Entity> selectEntityMap(Entity entity) throws DataServiceException;

	/**
	 * 查找列表，将实体的主键作为KEY输出为MAP
	 * 
	 * @param entity
	 * @return
	 * @throws DataServiceException
	 */
	Map<Long, Entity> selectEntityMap(Map<String, Object> parameter) throws DataServiceException;

	Entity selectEntity(Map<String, Object> parameter) throws DataServiceException;

	Entity selectEntity(Map<String, Object> parameter, Entity def) throws DataServiceException;

	/**
	 * <p>
	 * 插入一条记录（选择字段，策略插入）
	 * </p>
	 *
	 * @param entity 实体对象
	 */
	boolean save(Entity entity);

	/**
	 * <p>
	 * 插入（批量），该方法不适合 Oracle
	 * </p>
	 *
	 * @param entityList 实体对象集合
	 */
	default boolean saveBatch(Collection<Entity> entityList) {
		return saveBatch(entityList, 30);
	}

	/**
	 * <p>
	 * 插入（批量）
	 * </p>
	 *
	 * @param entityList 实体对象集合
	 * @param batchSize  插入批次数量
	 */
	boolean saveBatch(Collection<Entity> entityList, int batchSize);

	/**
	 * <p>
	 * 批量修改插入
	 * </p>
	 *
	 * @param entityList 实体对象集合
	 */
	boolean saveOrUpdateBatch(Collection<Entity> entityList);

	/**
	 * <p>
	 * 批量修改插入
	 * </p>
	 *
	 * @param entityList 实体对象集合
	 * @param batchSize  每次的数量
	 */
	boolean saveOrUpdateBatch(Collection<Entity> entityList, int batchSize);

	/**
	 * <p>
	 * 根据 ID 删除
	 * </p>
	 *
	 * @param id 主键ID
	 */
	boolean removeById(Serializable id);

	/**
	 * <p>
	 * 根据 columnMap 条件，删除记录
	 * </p>
	 *
	 * @param columnMap 表字段 map 对象
	 */
	boolean removeByMap(Map<String, Object> columnMap);

	/**
	 * <p>
	 * 删除（根据ID 批量删除）
	 * </p>
	 *
	 * @param idList 主键ID列表
	 */
	boolean removeByIds(Collection<? extends Serializable> idList);

	/**
	 * <p>
	 * 根据 ID 选择修改
	 * </p>
	 *
	 * @param entity 实体对象
	 */
	boolean updateById(Entity entity);

	/**
	 * <p>
	 * 根据ID 批量更新
	 * </p>
	 *
	 * @param entityList 实体对象集合
	 */
	default boolean updateBatchById(Collection<Entity> entityList) {
		return updateBatchById(entityList, 30);
	}

	/**
	 * <p>
	 * 根据ID 批量更新
	 * </p>
	 *
	 * @param entityList 实体对象集合
	 * @param batchSize  更新批次数量
	 */
	boolean updateBatchById(Collection<Entity> entityList, int batchSize);

	/**
	 * <p>
	 * TableId 注解存在更新记录，否插入一条记录
	 * </p>
	 *
	 * @param entity 实体对象
	 */
	boolean saveOrUpdate(Entity entity);

	boolean insert(Entity entity);

	boolean deleteById(Long id) throws DataServiceException;

	List<Entity> selectByIds(Collection<Long> ids) throws DataServiceException;

//	DataPaging<Entity> selectEntitys(String sqlStatement, Map<String, Object> expand, PageForm pageForm)
//			throws DataServiceException;
//
//	List<Entity> selectEntitys(String sqlStatement, Map<String, Object> params) throws DataServiceException;

}
