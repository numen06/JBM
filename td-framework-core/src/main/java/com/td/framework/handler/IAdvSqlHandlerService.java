package com.td.framework.handler;

import java.util.Map;

import com.td.framework.metadata.exceptions.ServiceException;
import com.td.framework.metadata.usage.bean.BaseEntity;
import com.td.framework.service.IAdvSqlService;

public interface IAdvSqlHandlerService<Entity extends BaseEntity> extends IAdvHandlerService<Entity, Long>, IAdvSqlService<Entity, Long> {
	/**
	 * 将主键作为Key实体作为Value组成Map
	 * 
	 * @param parameter
	 * @return
	 */
	public Map<Long, Entity> selectEntityMap(Entity parameter) throws ServiceException;

	/**
	 * 将主键作为Key实体作为Value组成Map
	 * 
	 * @param parameter
	 * @return
	 */
	public <K, V> Map<Long, Entity> selectEntityMap(Map<K, V> parameter) throws ServiceException;

	/**
	 * 通过主键查询实体
	 *
	 * @param id
	 * @return
	 */
	public Entity selectByPrimaryKey(Long id) throws ServiceException;

	/**
	 * 通过主键删除实体
	 * 
	 * @param id
	 * @return
	 */
	public Long deleteByPrimaryKey(Long id) throws ServiceException;
}
