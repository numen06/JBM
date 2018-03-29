package com.td.framework.service;

import java.io.Serializable;
import java.util.Map;

public interface IAdvMySqlService<Entity extends Serializable, PK extends Serializable> extends IAdvService<Entity, PK>, IBaseMySqlService<Entity> {
	public Map<PK, Entity> selectEntityMap(Entity parameter);

	public <K, V> Map<PK, Entity> selectEntityMap(Map<K, V> parameter);

	public Entity selectByPrimaryKey(PK id);

	public Integer deleteByPrimaryKey(PK id);

	public PK saveEntity(Entity entity);
}
