package com.td.framework.handler.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.td.framework.handler.IBaseMySqlHandlerService;
import com.td.framework.metadata.usage.page.DataPaging;
import com.td.framework.metadata.usage.page.PageForm;
import com.td.framework.service.IBaseMySqlService;

public abstract class BaseMySqlHandlerService<Entity extends Serializable, Service extends IBaseMySqlService<Entity>> implements IBaseMySqlHandlerService<Entity> {

	protected Service privateService;

	@Autowired(required = false)
	public void setPrivateService(Service privateService) {
		this.privateService = privateService;
	}

	@Override
	public Long save(Entity entity) {
		return this.privateService.save(entity);
	}

	@Override
	public Integer delete(Entity entity) {
		return this.privateService.delete(entity);
	}

	@Override
	public Long update(Entity entity) {
		return this.privateService.update(entity);
	}

	@Override
	public Entity selectEntity(Entity entity) {
		return this.privateService.selectEntity(entity);
	}

	@Override
	public <K, V> Entity selectEntity(Map<K, V> params) {
		return this.privateService.selectEntity(params);
	}

	@Override
	public List<Entity> selectEntitys(Entity entity) {
		return this.privateService.selectEntitys(entity);
	}

	@Override
	public <K, V> List<Entity> selectEntitys(Map<K, V> params) {
		return this.privateService.selectEntitys(params);
	}

	@Override
	public DataPaging<Entity> selectEntitys(Entity entity, PageForm pageForm) {
		return this.privateService.selectEntitys(entity, pageForm);
	}

	@Override
	public <K, V> DataPaging<Entity> selectEntitys(Map<K, V> params, PageForm pageForm) {
		return this.privateService.selectEntitys(params, pageForm);
	}

	@Override
	public Long insertBatch(Collection<Entity> records) {
		return this.privateService.insertBatch(records);
	}

	@Override
	public Long insert(Entity entity) {
		return this.privateService.insert(entity);
	}

	@Override
	public DataPaging<Entity> selectEntitys(Entity entity, Map<String, Object> expand, PageForm pageForm) {
		return this.privateService.selectEntitys(entity, expand, pageForm);
	}

	@Override
	public List<Entity> selectEntitys(Entity entity, Map<String, Object> expand) {
		return this.privateService.selectEntitys(entity, expand);
	}

}
