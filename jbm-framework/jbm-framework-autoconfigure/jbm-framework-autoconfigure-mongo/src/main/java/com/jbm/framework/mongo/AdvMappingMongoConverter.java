package com.jbm.framework.mongo;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoTypeMapper;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;

/**
 * 高级的Mongo数据转换类
 *
 * @author wesley
 */
public class AdvMappingMongoConverter extends MappingMongoConverter {

    @SuppressWarnings("deprecation")
    public AdvMappingMongoConverter(MongoDbFactory mongoDbFactory, MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
        super(mongoDbFactory, mappingContext);
    }

    public AdvMappingMongoConverter(DbRefResolver dbRefResolver, MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
        super(dbRefResolver, mappingContext);
    }

    /**
     * 默认将_class去掉
     *
     * @param mongoDbFactory
     */
    @SuppressWarnings("deprecation")
    public AdvMappingMongoConverter(MongoDbFactory mongoDbFactory) {
        super(mongoDbFactory, new MongoMappingContext());
        MongoTypeMapper typeMapper = new DefaultMongoTypeMapper(null);
        this.setTypeMapper(typeMapper);
    }

}
