package com.jbm.framework.mongo;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;

import com.jbm.framework.metadata.usage.bean.AdvMongoBean;
import com.jbm.framework.mongo.rep.AdvMongoRepository;

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
