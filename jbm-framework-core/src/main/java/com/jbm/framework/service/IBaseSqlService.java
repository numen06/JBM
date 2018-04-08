package com.jbm.framework.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.service.IService;
import com.jbm.framework.metadata.exceptions.ServiceException;
import com.jbm.framework.metadata.usage.bean.PrimaryKey;
import com.jbm.framework.metadata.usage.page.DataPaging;
import com.jbm.framework.metadata.usage.page.PageForm;

public interface IBaseSqlService<Entity extends PrimaryKey<PK>, PK extends Serializable> extends IBaseService<Entity>, IService<Entity> {
	/**
	 * 保存（持久化）对象
	 * 
	 * @return 执行成功数
	 */
	public Entity save(Entity entity) throws ServiceException;

	/**
	 * 保存（持久化）对象
	 * 
	 * @return 执行成功数
	 */
	public boolean save(List<Entity> entitys) throws ServiceException;

	// /**
	// * 插入一个实体
	// *
	// * @param entity
	// * @return
	// */
	// public Long insert(Entity entity) throws ServiceException;

	/**
	 * 删除（持久化）对象
	 * 
	 * @param entity
	 * @return
	 */
	public boolean delete(Entity entity) throws ServiceException;

	/**
	 * 更新（持久化）对象
	 * 
	 * @param entity
	 *            持久化对象
	 * @return 执行成功的记录个数
	 */
	public boolean update(Entity entity) throws ServiceException;

	/**
	 * 通过实体去查询其他信息一般用于ID
	 * 
	 * @param entity
	 * @return
	 */
	public Entity selectEntity(Entity entity) throws ServiceException;

	/**
	 * 
	 * 通过Map查询实体
	 * 
	 * @param params
	 * @return
	 */
	public <K, V> Entity selectEntity(Map<K, V> params) throws ServiceException;

	/**
	 * 不分页查询
	 * 
	 * @param entity
	 * @return 查询结果
	 */
	public List<Entity> selectEntitys(Entity entity) throws ServiceException;

	/**
	 * 不分页查询
	 * 
	 * @return 查询结果
	 */
	public <K, V> List<Entity> selectEntitys(Map<K, V> params) throws ServiceException;

	/**
	 * 查询实体
	 * 
	 * @param entity
	 * @param pageable
	 * @return
	 */
	public DataPaging<Entity> selectEntitys(Entity entity, PageForm pageForm) throws ServiceException;

	/**
	 * 
	 * 分页查询实体
	 * 
	 * @param params
	 * @param pageable
	 * @return
	 */
	public <K, V> DataPaging<Entity> selectEntitys(Map<K, V> params, PageForm pageForm) throws ServiceException;

	// /**
	// *
	// * 批量插入返回最后一的插入值的ID
	// *
	// * @param records
	// * @return
	// */
	// public Long insertBatch(Collection<Entity> records) throws
	// ServiceException;

	/**
	 * 通过实体查询分页，并且加入扩展字段
	 * 
	 * @param entity
	 *            实体
	 * @param expand
	 *            扩展字段
	 * @param pageable
	 *            分页信息
	 * @return
	 */
	DataPaging<Entity> selectEntitys(Entity entity, Map<String, Object> expand, PageForm pageForm) throws ServiceException;

	/**
	 * 通过实体查询列表，并且加入扩展
	 * 
	 * @param entity
	 *            实体
	 * @param expand
	 *            扩展
	 * @return
	 */
	List<Entity> selectEntitys(Entity entity, Map<String, Object> expand) throws ServiceException;

	<T> List<T> selectMapperList(String statement, Object... args) throws ServiceException;

	<T> T selectMapperOne(String statement, Object... args) throws ServiceException;

	<K, V> Map<K, V> selectMapperMap(String statement, String mapKey, Object... args) throws ServiceException;

	Entity selectByPrimaryKey(PK id) throws ServiceException;

}
