package com.jbm.framework.handler.service;

import java.io.Serializable;
import java.util.Map;

import com.jbm.framework.handler.IAdvMySqlHandlerService;
import com.jbm.framework.service.IAdvMySqlService;

public abstract class AdvMySqlHandlerService<Service extends IAdvMySqlService<Entity, PK>, Entity extends Serializable, PK extends Serializable>
	extends BaseMySqlHandlerService<Entity, Service>implements IAdvMySqlHandlerService<Entity, PK> {

	@Override
	public Map<PK, Entity> selectEntityMap(Entity parameter) {
		return super.privateService.selectEntityMap(parameter);
	}

	@Override
	public <K, V> Map<PK, Entity> selectEntityMap(Map<K, V> parameter) {
		return super.privateService.selectEntityMap(parameter);
	}

	@Override
	public Entity selectByPrimaryKey(PK id) {
		return super.privateService.selectByPrimaryKey(id);
	}

	@Override
	public Integer deleteByPrimaryKey(PK id) {
		return super.privateService.deleteByPrimaryKey(id);
	}

	@Override
	public PK saveEntity(Entity entity) {
		return super.privateService.saveEntity(entity);
	}

}
