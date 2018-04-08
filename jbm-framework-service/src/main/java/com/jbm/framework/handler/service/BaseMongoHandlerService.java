package com.jbm.framework.handler.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.jbm.framework.handler.IBaseMongoHandlerService;
import com.jbm.framework.metadata.exceptions.ServiceException;
import com.jbm.framework.metadata.usage.page.DataPaging;
import com.jbm.framework.metadata.usage.page.PageForm;
import com.jbm.framework.service.IBaseMongoService;

public abstract class BaseMongoHandlerService<Entity extends Serializable, Service extends IBaseMongoService<Entity>> implements IBaseMongoHandlerService<Entity> {

	/**
	 * logger实例
	 */
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * 默认注入私有服务
	 */
	protected Service privateService;

	@Autowired(required = false)
	public void setPrivateService(Service privateService) throws ServiceException {
		this.privateService = privateService;
	}

	@Override
	public Entity save(Entity entity) throws ServiceException {
		return this.privateService.save(entity);
	}

	@Override
	public List<Entity> save(List<Entity> entitys) throws ServiceException {
		return this.privateService.save(entitys);
	}

	@Override
	public Long delete(Entity entity) throws ServiceException {
		return this.privateService.delete(entity);
	}

	@Override
	public Long update(Entity entity) throws ServiceException {
		return this.privateService.update(entity);
	}

	@Override
	public Long update(Entity queryEntity, Entity updateEntity) throws ServiceException {
		return this.privateService.update(queryEntity, updateEntity);
	}

	@Override
	public Entity selectEntity(Entity entity) throws ServiceException {
		return this.privateService.selectEntity(entity);
	}

	@Override
	public <K, V> Entity selectEntity(Map<K, V> params) throws ServiceException {
		return this.privateService.selectEntity(params);
	}

	@Override
	public List<Entity> selectEntitys(Entity entity) throws ServiceException {
		return this.privateService.selectEntitys(entity);
	}

	@Override
	public <K, V> List<Entity> selectEntitys(Map<K, V> params) throws ServiceException {
		return this.privateService.selectEntitys(params);
	}

	@Override
	public DataPaging<Entity> selectEntitys(Entity entity, PageForm pageForm) throws ServiceException {
		return this.privateService.selectEntitys(entity, pageForm);
	}

	@Override
	public <K, V> DataPaging<Entity> selectEntitys(Map<K, V> params, PageForm pageForm) throws ServiceException {
		return this.privateService.selectEntitys(params, pageForm);
	}

	@Override
	public Long insertBatch(Collection<Entity> records) throws ServiceException {
		return this.privateService.insertBatch(records);
	}

	@Override
	public Long insert(Entity entity) throws ServiceException {
		return this.privateService.insert(entity);
	}

	@Override
	public DataPaging<Entity> selectEntitys(Entity entity, Map<String, Object> expand, PageForm pageForm) throws ServiceException {
		return this.privateService.selectEntitys(entity, expand, pageForm);
	}

	@Override
	public List<Entity> selectEntitys(Entity entity, Map<String, Object> expand) throws ServiceException {
		return this.privateService.selectEntitys(entity, expand);
	}

	@Override
	public Long selectCount(Entity entity) throws ServiceException {
		return this.privateService.selectCount(entity);
	}

}
