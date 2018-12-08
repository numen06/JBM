package com.jbm.framework.mongo;

import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

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

	public <T> List<T> doFind(String collectionName, String jsonCommand, Document fields, Class<T> entityClass) {
		Document query = Document.parse(jsonCommand);
		return super.doFind(collectionName, query, fields, entityClass);
	}

}
