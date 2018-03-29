package com.td.framework.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.td.framework.metadata.exceptions.ServiceException;
import com.td.framework.metadata.usage.page.DataPaging;
import com.td.framework.metadata.usage.page.PageForm;

public interface IBaseMongoService<Entity extends Serializable> extends IBaseService<Entity> {
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
	public List<Entity> save(List<Entity> entitys) throws ServiceException;

	/**
	 * 插入一个实体
	 * 
	 * @param entity
	 * @return
	 */
	public Long insert(Entity entity) throws ServiceException;

	/**
	 * 删除（持久化）对象
	 * 
	 * @param entity
	 * @return
	 */
	public Long delete(Entity entity) throws ServiceException;

	/**
	 * 更新（持久化）对象
	 * 
	 * @param entity
	 *            持久化对象
	 * @return 执行成功的记录个数
	 */
	public Long update(Entity entity) throws ServiceException;

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

	/**
	 * 
	 * 批量插入返回最后一的插入值的ID
	 * 
	 * @param records
	 * @return
	 */
	public Long insertBatch(Collection<Entity> records) throws ServiceException;

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
	public DataPaging<Entity> selectEntitys(Entity entity, Map<String, Object> expand, PageForm pageForm) throws ServiceException;

	/**
	 * 通过实体查询列表，并且加入扩展
	 * 
	 * @param entity
	 *            实体
	 * @param expand
	 *            扩展
	 * @return
	 */
	public List<Entity> selectEntitys(Entity entity, Map<String, Object> expand) throws ServiceException;

	/**
	 * 
	 * 通过实体统计行数有多少
	 * 
	 * @param entity
	 * @return
	 * @throws ServiceException
	 */
	public Long selectCount(Entity entity) throws ServiceException;

	public Long update(Entity queryEntity, Entity updateEntity) throws ServiceException;

}
