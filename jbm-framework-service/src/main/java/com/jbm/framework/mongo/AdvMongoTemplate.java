package com.jbm.framework.mongo;

import java.util.List;

import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * MongoDb的实际操作类
 * 
 * @author wesley
 *
 */
public class AdvMongoTemplate extends MongoTemplate {

	public AdvMongoTemplate(MongoDbFactory mongoDbFactory) {
		super(mongoDbFactory, new AdvMappingMongoConverter(mongoDbFactory));
	}

	public <T> List<T> doFind(String collectionName, String jsonCommand, DBObject fields, Class<T> entityClass) {
		DBObject query = (DBObject) JSON.parse(jsonCommand);
		return super.doFind(collectionName, query, fields, entityClass);
	}

}
