package com.jbm.framework.handler;

import java.io.Serializable;
import java.util.Map;

import com.jbm.framework.metadata.exceptions.ServiceException;
import com.jbm.framework.service.IAdvMongoService;

public interface IAdvMongoHandlerService<Entity extends Serializable, PK extends Serializable> extends IAdvHandlerService<Entity, PK>, IAdvMongoService<Entity, PK> {
	/**
	 * 将主键作为Key实体作为Value组成Map
	 * 
	 * @param parameter
	 * @return
	 */
	public Map<PK, Entity> selectEntityMap(Entity parameter) throws ServiceException;

	/**
	 * 将主键作为Key实体作为Value组成Map
	 * 
	 * @param parameter
	 * @return
	 */
	public <K, V> Map<PK, Entity> selectEntityMap(Map<K, V> parameter) throws ServiceException;

	/**
	 * 通过主键查询实体
	 * 
	 * @param id
	 * @return
	 */
	public Entity selectByPrimaryKey(PK id) throws ServiceException;

	/**
	 * 通过主键删除实体
	 * 
	 * @param id
	 * @return
	 */
	public Long deleteByPrimaryKey(PK id) throws ServiceException;
}
