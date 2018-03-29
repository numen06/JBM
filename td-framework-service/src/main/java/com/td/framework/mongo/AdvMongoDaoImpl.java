package com.td.framework.mongo;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;

import com.td.framework.metadata.usage.bean.AdvMongoBean;
import com.td.framework.mongo.rep.AdvMongoRepository;

/**
 * 
 * 高级Mongo基础服务
 * 
 * @author wesley
 *
 * @param <RESP>
 * @param <Entity>
 * @param <PK>
 */
public class AdvMongoDaoImpl<RESP extends AdvMongoRepository<Entity, CODE>, Entity extends AdvMongoBean<CODE>, CODE extends Serializable> extends BaseMongoDaoImpl<Entity, String> {

	@Autowired
	protected RESP privateRepository;

}
