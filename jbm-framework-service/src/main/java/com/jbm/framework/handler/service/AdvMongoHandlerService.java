package com.jbm.framework.handler.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.jbm.framework.handler.IAdvMongoHandlerService;
import com.jbm.framework.metadata.exceptions.ServiceException;
import com.jbm.framework.service.IAdvMongoService;

/**
 * 高级实现类
 * 
 * @author wesley
 *
 * @param <Service>
 * @param <Entity>
 * @param <PK>
 */
public abstract class AdvMongoHandlerService<Service extends IAdvMongoService<Entity, PK>, Entity extends Serializable, PK extends Serializable>
	extends BaseMongoHandlerService<Entity, Service>implements IAdvMongoHandlerService<Entity, PK> {

	@Override
	public Map<PK, Entity> selectEntityMap(Entity parameter) throws ServiceException {
		return super.privateService.selectEntityMap(parameter);
	}

	@Override
	public <K, V> Map<PK, Entity> selectEntityMap(Map<K, V> parameter) throws ServiceException {
		return super.privateService.selectEntityMap(parameter);
	}

	@Override
	public Entity selectByPrimaryKey(PK id) throws ServiceException {
		return super.privateService.selectByPrimaryKey(id);
	}

	@Override
	public Long deleteByPrimaryKey(PK id) throws ServiceException {
		return super.privateService.deleteByPrimaryKey(id);
	}

	@Override
	public Entity save(Entity entity) throws ServiceException {
		return super.privateService.save(entity);
	}

	@Override
	public List<Entity> selectByPrimaryKeys(Iterable<PK> ids) throws ServiceException {
		return super.privateService.selectByPrimaryKeys(ids);
	}

	@Override
	public Long deleteByPrimaryKeys(Iterable<PK> ids) throws ServiceException {
		return super.privateService.deleteByPrimaryKeys(ids);
	}

}
