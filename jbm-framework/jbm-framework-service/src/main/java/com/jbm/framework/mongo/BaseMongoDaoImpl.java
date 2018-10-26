package com.jbm.framework.mongo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.WriteResult;
import com.jbm.framework.metadata.exceptions.DataServiceException;
import com.jbm.framework.metadata.usage.bean.PrimaryKey;
import com.jbm.framework.metadata.usage.page.DataPaging;
import com.jbm.framework.metadata.usage.page.PageForm;
import com.jbm.util.CollectionUtils;
import com.jbm.util.MapUtils;
import com.jbm.util.ObjectUtils;
import com.jbm.util.map.SetHashMap;

public abstract class BaseMongoDaoImpl<Entity extends PrimaryKey<PK>, PK extends Serializable> implements IBaseMongoDao<Entity, PK> {

	protected final static Logger privateLogger = LoggerFactory.getLogger(BaseMongoDaoImpl.class);

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	private AdvMongoTemplate mongoTemplate;

	protected String collectionName;

	private MongoRepository<Entity, PK> simpleMongoRepository;

	@SuppressWarnings("unchecked")
	@Autowired
	public void setMongoTemplate(AdvMongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
		this.collectionName = mongoTemplate.getCollectionName(this.entityDaoWarp.getEntityClass());
		// 判断是否存在表，不存在的话创建
		if (!this.mongoTemplate.collectionExists(this.collectionName))
			this.mongoTemplate.createCollection(this.collectionName);
		MongoRepositoryFactory mongoRepositoryFactory = new MongoRepositoryFactory(this.mongoTemplate);
		this.simpleMongoRepository = new SimpleMongoRepository<Entity, PK>(
			(MongoEntityInformation<Entity, PK>) mongoRepositoryFactory.getEntityInformation(this.entityDaoWarp.getEntityClass()), this.mongoTemplate);
	}

	public AdvMongoTemplate getMongoTemplate() {
		return mongoTemplate;
	}

	protected String getCollectionName() {
		return this.collectionName;
	}

	/**
	 * 对Dao层设置的实体封装的一个类
	 */
	private EntityDaoWarp<Entity> entityDaoWarp;

	public EntityDaoWarp<Entity> getEntityDaoWarp() {
		return entityDaoWarp;
	}

	@SuppressWarnings("unchecked")
	public BaseMongoDaoImpl() {
		super();
		if (this instanceof AdvMongoDaoImpl)
			this.entityDaoWarp = DaoUtils.reflectRepositoryBaseDaoWarp(this.getClass());
		else
			this.entityDaoWarp = DaoUtils.reflectEntityBaseDaoWarp(this.getClass());
	}

	@Override
	public Long delete(Entity entity) throws DataServiceException {
		WriteResult reuslt = null;
		if (ObjectUtils.isNotNull(entity.getId())) {
			reuslt = this.mongoTemplate.remove(entity);
		} else {
			Query query = this.buildMapQuery(entity);
			reuslt = this.mongoTemplate.remove(query, this.getEntityDaoWarp().getEntityClass());
		}
		return new Long(reuslt.getN());
	}

	@Override
	public Long deleteByPrimaryKey(PK id) throws DataServiceException {
		try {
			this.simpleMongoRepository.delete(id);
		} catch (Exception e) {
			return 0l;
		}
		return 1l;
	}

	@Override
	public Long insert(Entity record) throws DataServiceException {
		try {
			this.simpleMongoRepository.insert(record);
			return 1l;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0l;
	}

	@Override
	public Entity selectByPrimaryKey(PK id) throws DataServiceException {
		return this.simpleMongoRepository.findOne(id);
	}

	@Override
	public Long updateByPrimaryKey(Entity entity) throws DataServiceException {
		Entity dbEntity = this.selectByPrimaryKey(entity.getId());
		if (dbEntity == null)
			return 0l;
		WriteResult result = this.mongoTemplate.updateFirst(this.buildMapQuery(entity), buildEntityUpdate(dbEntity, entity), this.getEntityDaoWarp().getEntityClass());
		return new Long(result.getN());
	}

	protected <K, V> Update buildEntityUpdate(Entity dbEntity, Entity entity) {
		Map<String, Object> dbMap = this.entityToMap(dbEntity);
		Map<String, Object> map = this.entityToMap(entity);
		dbMap.putAll(map);
		return this.buildEntityUpdate(dbMap);
	}

	protected <K, V> Update buildEntityUpdate(Entity entity) {
		return this.buildEntityUpdate(this.entityToMap(entity));
	}

	protected <K, V> Update buildEntityUpdate(Map<K, V> params, String... exclude) {
		// Update update = Update.fromDBObject(new BasicDBObject(params), "id");
		Update update = new Update();
		exclude = (String[]) ArrayUtils.add(exclude, "id");
		List<String> excludeList = Arrays.asList(exclude);
		for (K key : params.keySet()) {
			if (excludeList.contains(key))
				continue;
			V value = params.get(key);
			if (value == null) {
				continue;
			} else if (value instanceof Collection) {
				if (CollectionUtils.isEmpty((Collection<?>) value))
					continue;
			}
			update.set(key.toString(), value);

		}
		return update;
	}

	@Override
	public Entity selectEntity(Entity entity) throws DataServiceException {
		Query query = this.buildMapQuery(entity);
		return findEntityFormMongo(query);
	}

	protected List<Entity> findEntitysFormMongo(Query query) {
		return this.mongoTemplate.find(query, this.entityDaoWarp.getEntityClass());
	}

	protected Entity findEntityFormMongo(Query query) {
		List<Entity> list = findEntitysFormMongo(query);
		return CollectionUtils.firstResult(list);
	}

	@Override
	public List<Entity> selectEntitys(Entity entity) throws DataServiceException {
		Query query = this.buildMapQuery(entity);
		List<Entity> list = findEntitysFormMongo(query);
		return list;
	}

	@Override
	public List<Entity> selectAllEntitys() throws DataServiceException {
		return this.simpleMongoRepository.findAll();
	}

	@Override
	public <K, V> List<Entity> selectEntitys(Map<K, V> params) throws DataServiceException {
		return findEntitysFormMongo(this.buildMapQuery(params));
	}

	protected <K, V> Query buildMapQuery(Entity entity) {
		Query query = null;
		if (entity.getId() == null)
			query = buildMapQuery(entityToMap(entity));
		else
			query = new Query(Criteria.where("_id").is(entity.getId()));
		return query;
	}

	protected <K, V> Criteria diyCriteria(Map<K, V> params, Criteria criteria) {
		return criteria;
	}

	protected <K, V> Query buildMapQuery(Map<K, V> params, String... exclude) {
		Criteria criteria = Criteria.where("");
		List<String> excludeList = Arrays.asList(exclude);
		this.diyCriteria(params, criteria);
		for (K key : params.keySet()) {
			if (excludeList.contains(key))
				continue;
			Object value = params.get(key);
			if (params instanceof SetHashMap) {
				value = ((SetHashMap<K, V>) params).getList(key);
			}
			if (value == null)
				continue;
			if (value instanceof Collection) {
				if (CollectionUtils.isNotEmpty((Collection<?>) value)) {
					Collection<?> col = (Collection<?>) value;
					if (CollectionUtils.length(col) > 1) {
						criteria.and(key.toString()).in(col);
					} else {
						for (Object object : col) {
							criteria.and(key.toString()).is(object);
						}
					}
				}
			} else {
				criteria.and(key.toString()).is(value);
			}
		}
		return new Query(criteria);
	}

	public <K, V> DataPaging<Entity> selectEntitys(Map<K, V> params, PageForm pageForm) throws DataServiceException {
		Query query = this.buildMapQuery(params);
		Long total = findPagingTotal(query);
		query.with(pageForm.pageable());
		List<Entity> list = this.mongoTemplate.find(query, this.entityDaoWarp.getEntityClass());
		return new DataPaging<Entity>(list, total, pageForm.pageable());
	}

	@Override
	public Long selectCount(Entity entity) {
		Query query = this.buildMapQuery(entity);
		Long total = findPagingTotal(query);
		return total;
	}

	public <K, V> Long selectCount(Map<K, V> params) {
		Query query = this.buildMapQuery(params);
		Long total = findPagingTotal(query);
		return total;
	}

	/**
	 * 查询总数
	 * 
	 * @param query
	 * @return
	 */
	protected Long findPagingTotal(Query query) {
		Long total = 0l;
		try {
			total = this.mongoTemplate.count(query, this.entityDaoWarp.getEntityClass());
		} catch (Exception e) {
			this.logger.error("查询分页总数错误", e);
		}
		return total;
	}

	protected DataPaging<Entity> selectEntitys(Query query, PageForm pageForm) throws DataServiceException {
		Long total = this.findPagingTotal(query);
		if (total <= 0l)
			return new DataPaging<Entity>(new ArrayList<Entity>(), total, pageForm.pageable());
		// 如果有pageForm,加入查询排序过滤
		query.with(pageForm.pageable());
		List<Entity> result = this.getMongoTemplate().find(query, this.entityDaoWarp.getEntityClass());
		return new DataPaging<Entity>(result, total, pageForm.pageable());
	}

	@Override
	public <K, V> Entity selectEntity(Map<K, V> parameter) throws DataServiceException {
		return this.findEntityFormMongo(this.buildMapQuery(parameter));
	}

	@Override
	public Long insertBatch(Collection<Entity> record) throws DataServiceException {
		try {
			this.getMongoTemplate().insert(record);
			return new Long(CollectionUtils.length(record));
		} catch (Exception e) {
			this.logger.error("批量插入错误", e);
			return 0l;
		}
	}

	@Override
	public Entity save(Entity entity) throws DataServiceException {
		this.mongoTemplate.save(entity);
		return entity;
	}

	@Override
	public List<Entity> save(List<Entity> entity) throws DataServiceException {
		return this.simpleMongoRepository.save(entity);
	}

	@Override
	public Long update(Entity entity) throws DataServiceException {
		WriteResult result = null;
		if (entity.getId() == null)
			result = this.mongoTemplate.updateMulti(this.buildMapQuery(entity), this.buildEntityUpdate(entity), this.entityDaoWarp.getEntityClass());
		else
			return this.updateByPrimaryKey(entity);
		return new Long(result.getN());
	}

	@Override
	public Long update(Entity queryEntity, Entity updateEntity) throws DataServiceException {
		WriteResult result = null;
		if (queryEntity.getId() == null)
			result = this.mongoTemplate.updateMulti(this.buildMapQuery(queryEntity), this.buildEntityUpdate(updateEntity), this.entityDaoWarp.getEntityClass(),
				this.getCollectionName());
		else
			return this.updateByPrimaryKey(queryEntity);
		return new Long(result.getN());
	}

	public Map<PK, Entity> selectEntityMap(Entity entity) throws DataServiceException {
		List<Entity> list = this.selectEntitys(entity);
		Map<PK, Entity> result = Maps.newHashMap();
		for (Iterator<Entity> iterator = list.iterator(); iterator.hasNext();) {
			Entity e = iterator.next();
			result.put(e.getId(), e);
		}
		return result;
	}

	public <K, V> Map<PK, Entity> selectEntityMap(Map<K, V> parameter) throws DataServiceException {
		List<Entity> list = this.selectEntitys(parameter);
		Map<PK, Entity> result = Maps.newHashMap();
		for (Iterator<Entity> iterator = list.iterator(); iterator.hasNext();) {
			Entity e = iterator.next();
			result.put(e.getId(), e);
		}
		return result;
	}

	/**
	 * 将实体转换成为Map以便于对Mapper进行操作
	 * 
	 * @param entity
	 * @return
	 */
	protected Map<String, Object> entityToMap(Entity entity) {
		Map<String, Object> entityMap = MapUtils.newHashMap();
		try {
			entityMap = ObjectUtils.foreachObject(entity);
		} catch (Exception e) {
			logger.error("将实体转换成Map错误", e);
			throw new RuntimeException(e);
		}
		return entityMap;
	}

	@Override
	public DataPaging<Entity> selectEntitys(Entity entity, PageForm pageForm) throws DataServiceException {
		return this.selectEntitys(entityToMap(entity), pageForm);
	}

	@Override
	public <K, V> Entity selectOnlyEntity(Map<K, V> parameter) throws DataServiceException {
		return this.selectEntity(parameter);
	}

	@Override
	public Entity selectOnlyEntity(Entity entity) throws DataServiceException {
		return this.selectEntity(entity);
	}

	@Override
	public <K, V> Entity selectEntity(Map<K, V> parameter, Entity def) throws DataServiceException {
		Entity result = this.selectEntity(parameter);
		if (result == null)
			return def;
		return result;
	}

	@Override
	public Entity selectEntity(Entity entity, Entity def) throws DataServiceException {
		Entity result = this.selectEntity(entity);
		if (result == null)
			return def;
		return result;
	}

	@Override
	public DataPaging<Entity> selectEntitys(Entity entity, Map<String, Object> expand, PageForm pageForm) throws DataServiceException {
		Map<String, Object> params = this.entityToMap(entity);
		params.putAll(expand);
		Query query = this.buildMapQuery(params);
		Long total = findPagingTotal(query);
		query.with(pageForm.pageable());
		List<Entity> list = this.mongoTemplate.find(query, this.entityDaoWarp.getEntityClass());
		return new DataPaging<Entity>(list, total, pageForm.pageable());
	}

	@Override
	public List<Entity> selectEntitys(Entity entity, Map<String, Object> expand) throws DataServiceException {
		Map<String, Object> params = this.entityToMap(entity);
		params.putAll(expand);
		return this.selectEntitys(params);
	}

	@Override
	public List<Entity> selectByPrimaryKeys(Iterable<PK> ids) throws DataServiceException {
		return Lists.newArrayList(this.simpleMongoRepository.findAll(ids));
	}

	@Override
	public Long deleteByPrimaryKeys(Iterable<PK> ids) throws DataServiceException {
		long rest = 0l;
		for (Iterator<PK> iterator = ids.iterator(); iterator.hasNext();) {
			PK id = iterator.next();
			this.simpleMongoRepository.delete(id);
			rest++;
		}
		return rest;
	}

}
