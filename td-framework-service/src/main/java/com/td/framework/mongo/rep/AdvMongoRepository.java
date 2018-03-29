package com.td.framework.mongo.rep;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.td.framework.metadata.usage.bean.AdvMongoBean;

public interface AdvMongoRepository<Entity extends AdvMongoBean<CODE>, CODE extends Serializable> extends MongoRepository<Entity, String> {

	@Override
	Page<Entity> findAll(Pageable pageable);

	@Override
	<S extends Entity> S save(S entity);

	@Override
	Entity findOne(String id);

	/**
	 * 通过CODE找到一个实体
	 * 
	 * @param code
	 * @return
	 */
	Entity findOneByCode(CODE code);

	@Override
	boolean exists(String id);

	@Override
	List<Entity> findAll(Iterable<String> ids);

	/**
	 * 通过多个CODE删除对象
	 * 
	 * @param codes
	 * @return
	 */
	List<Entity> findAllByCode(CODE code);

	@Override
	long count();

	@Override
	void delete(String id);

	/**
	 * 通过code删除对象
	 * 
	 * @param code
	 */
	long deleteByCode(CODE code);

	@Override
	void delete(Entity entity);

	@Override
	void delete(Iterable<? extends Entity> entities);

	@Override
	void deleteAll();

	@Override
	<S extends Entity> List<S> save(Iterable<S> entites);

	@Override
	List<Entity> findAll();

	@Override
	List<Entity> findAll(Sort sort);

	@Override
	<S extends Entity> S insert(S entity);

	@Override
	<S extends Entity> List<S> insert(Iterable<S> entities);

}
