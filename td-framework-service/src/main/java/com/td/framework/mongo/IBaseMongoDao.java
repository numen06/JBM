package com.td.framework.mongo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.td.framework.metadata.exceptions.DataServiceException;
import com.td.framework.metadata.usage.page.DataPaging;
import com.td.framework.metadata.usage.page.PageForm;

public interface IBaseMongoDao<Entity extends Serializable, PK extends Serializable> extends IBaseDao<Entity, PK> {
	/**
	 * 查询一个实体
	 * 
	 * @param statement
	 * @param parameter
	 * @return
	 */
	// <T> T selectOne(String statement, Object parameter) throws
	// DataServiceException;

	/**
	 * 查询实体List
	 * 
	 * @param statement
	 * @param parameter
	 * @return
	 */
	// <E> List<E> selectList(String statement, Object parameter) throws
	// DataServiceException;

	/**
	 * 查询实体List
	 * 
	 * @param statement
	 * @param parameter
	 * @return
	 */
	// List<Entity> selectEntitys(String statement, Object parameter) throws
	// DataServiceException;

	/**
	 * 
	 * 分页查询
	 * 
	 * @param statement
	 *            需要查询的mapper方法
	 * @param parameter
	 *            查询参数
	 * @param offset
	 *            数据开始节点
	 * @param limit
	 *            每页数据量
	 * @return 分页实体
	 * 
	 * @deprecated 推荐使用{@link PageForm}
	 */
	// <E> Page<E> selectPaging(String statement, Object parameter,
	// Integer offset, Integer limit) throws DataServiceException;

	/**
	 * 
	 * 分页查询
	 * 
	 * @param statement
	 *            需要查询的mapper方法
	 * @param parameter
	 *            查询参数
	 * @param offset
	 *            数据开始节点
	 * @param limit
	 *            每页数据量
	 * @return 分页实体
	 * 
	 * @deprecated 推荐使用{@link PageForm}
	 * 
	 */
	// Page<Entity> selectPagingEntitys(String statement, Object
	// parameter, Integer offset, Integer limit) throws DataServiceException;

	/**
	 * 
	 * 查询Map数据
	 * 
	 * @param statement
	 * @param parameter
	 * @param mapKey
	 * @return
	 */
	// <K, V> Map<K, V> selectMap(String statement, Object parameter, String
	// mapKey) throws DataServiceException;

	/**
	 * 查询Map封装数据的List
	 * 
	 * @param statement
	 * @param parameter
	 * @return
	 */
	// <K, V> List<Map<K, V>> selectMapList(String statement, Object parameter)
	// throws DataServiceException;

	/**
	 * 
	 * 查询分页列表
	 * 
	 * @param statement
	 * @param parameter
	 * @param offset
	 * @param limit
	 * @return
	 * @deprecated 推荐使用{@link PageForm}
	 */
	// <K, V> Page<Map<K, V>> selectPagingMapList(String statement, Object
	// parameter, Integer offset, Integer limit) throws DataServiceException;

	/**
	 * 删除实体
	 * 
	 * @param entity
	 * @return
	 */
	Long delete(Entity entity) throws DataServiceException;

	/**
	 * 通过主键删除实体
	 * 
	 * @param id
	 * @return
	 */
	Long deleteByPrimaryKey(PK id) throws DataServiceException;

	/**
	 * 插入实体
	 * 
	 * @param entity
	 * @return
	 */
	Long insert(Entity entity) throws DataServiceException;

	/**
	 * 有条件的插入实体
	 * 
	 * @param entity
	 * @return
	 */
	// Long insertSelective(Entity entity) throws DataServiceException;

	/**
	 * 通过主键查找实体
	 * 
	 * @param id
	 * @return
	 */
	Entity selectByPrimaryKey(PK id) throws DataServiceException;

	/**
	 * 有条件的通过主键更新实体
	 * 
	 * @param entity
	 * @return
	 */
	// Long updateByPrimaryKeySelective(Entity entity) throws
	// DataServiceException;

	/**
	 * 通过主键更新实体
	 * 
	 * @param entity
	 * @return
	 */
	Long updateByPrimaryKey(Entity entity) throws DataServiceException;

	/**
	 * 通过实体查询实体
	 * 
	 * @param entity
	 * @return
	 */
	Entity selectEntity(Entity entity) throws DataServiceException;

	/**
	 * 通过实体查询实体List
	 * 
	 * @param entity
	 * @return
	 */
	List<Entity> selectEntitys(Entity entity) throws DataServiceException;

	/**
	 * 通过实体查询分页数据
	 * 
	 * @param entity
	 * @param offset
	 * @param limit
	 * @return
	 * @deprecated 推荐使用{@link PageForm}
	 */
	// Page<Entity> selectEntitys(Entity entity, Integer offset, Integer
	// limit) throws DataServiceException;

	/**
	 * 通过Map数据查找实体
	 * 
	 * @param params
	 * @return
	 */
	<K, V> List<Entity> selectEntitys(Map<K, V> params) throws DataServiceException;

	/**
	 * 通过Map数据查找分页数据
	 * 
	 * @param params
	 * @param offset
	 * @param limit
	 * @return
	 * @deprecated 推荐使用{@link PageForm}
	 */
	// <K, V> Page<Entity> selectEntitys(Map<K, V> params, Integer offset,
	// Integer limit) throws DataServiceException;

	/**
	 * 通过Map数据查找实体
	 * 
	 * @param parameter
	 * @return
	 */
	<K, V> Entity selectEntity(Map<K, V> parameter) throws DataServiceException;

	/**
	 * 批量插入实体
	 * 
	 * @param entity
	 * @return
	 */
	Long insertBatch(Collection<Entity> entity) throws DataServiceException;

	/**
	 * 保存实体
	 * 
	 * @param entity
	 * @return
	 */
	Entity save(Entity entity) throws DataServiceException;

	/**
	 * 更新实体
	 * 
	 * @param entity
	 * @return
	 */
	Long update(Entity entity) throws DataServiceException;

	/**
	 * 通过实体查询MAP
	 * 
	 * @param entity
	 * @return
	 */
	Map<PK, Entity> selectEntityMap(Entity entity) throws DataServiceException;

	/**
	 * 通过参数查询实体
	 * 
	 * @param parameter
	 * @return
	 */
	<K, V> Map<PK, Entity> selectEntityMap(Map<K, V> parameter) throws DataServiceException;

	/**
	 * 通过实体查询分页
	 * 
	 * @param entity
	 * @param pageForm
	 * @return
	 */
	DataPaging<Entity> selectEntitys(Entity entity, PageForm pageForm) throws DataServiceException;

	/**
	 * 查询一个实体
	 * 
	 * @param parameter
	 * @return
	 */
	<K, V> Entity selectOnlyEntity(Map<K, V> parameter) throws DataServiceException;

	/**
	 * 
	 * @param entity
	 * @return
	 */
	Entity selectOnlyEntity(Entity entity) throws DataServiceException;

	/**
	 * 查询实体
	 * 
	 * @param parameter
	 * @param def
	 * @return
	 */
	<K, V> Entity selectEntity(Map<K, V> parameter, Entity def) throws DataServiceException;

	/**
	 * 查询实体
	 * 
	 * @param entity
	 * @param def
	 * @return
	 */
	Entity selectEntity(Entity entity, Entity def) throws DataServiceException;

	/**
	 * 通过实体查询分页，并且加入扩展字段
	 * 
	 * @param entity
	 *            实体
	 * @param expand
	 *            扩展字段
	 * @param pageForm
	 *            分页信息
	 * @return
	 */
	DataPaging<Entity> selectEntitys(Entity entity, Map<String, Object> expand, PageForm pageForm) throws DataServiceException;

	/**
	 * 通过实体查询列表，并且加入扩展
	 * 
	 * @param entity
	 *            实体
	 * @param expand
	 *            扩展
	 * @return
	 */
	List<Entity> selectEntitys(Entity entity, Map<String, Object> expand) throws DataServiceException;

	/**
	 * 查询所有实体
	 * 
	 * @return
	 */
	List<Entity> selectAllEntitys() throws DataServiceException;

	List<Entity> save(List<Entity> entity) throws DataServiceException;

	List<Entity> selectByPrimaryKeys(Iterable<PK> ids) throws DataServiceException;

	Long deleteByPrimaryKeys(Iterable<PK> ids) throws DataServiceException;

	Long selectCount(Entity entity) throws DataServiceException;

	Long update(Entity entity, Entity updateEntity) throws DataServiceException;
}
