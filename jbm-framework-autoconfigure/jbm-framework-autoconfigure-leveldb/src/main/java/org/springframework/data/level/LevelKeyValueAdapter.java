package org.springframework.data.level;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.data.keyvalue.core.AbstractKeyValueAdapter;
import org.springframework.data.level.serializer.LevelSerializer;
import org.springframework.data.util.CloseableIterator;
import org.springframework.util.Assert;

import jbm.framework.boot.autoconfigure.level.LevelProperties;

/**
 * @author welsey.zhang
 *
 */
public class LevelKeyValueAdapter extends AbstractKeyValueAdapter implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(LevelKeyValueAdapter.class);

	private final LevelTemplate<Object, Object> levelTemplate;

	private String cachedb;

	private Boolean autoDestroy;

	private LevelProperties option;

	// public LevelKeyValueAdapter(LevelTemplate<Object, Object> levelTemplate)
	// {
	// this.levelTemplate = levelTemplate;
	// }

	public LevelKeyValueAdapter(LevelTemplate<Object, Object> levelTemplate, LevelProperties option) {
		this.levelTemplate = levelTemplate;
		this.option = option;
	}

	// public <T> T execute(LevelCallback<T> callback) {
	// return levelTemplate.execute(dbName, callback);
	// }

	@Override
	public Object put(Serializable id, Object item, Serializable keyspace) {
		try {
			levelTemplate.put(createDbName(keyspace), id, item);
		} catch (Exception e) {
			return null;
		}
		return item;
	}

	@Override
	public boolean contains(Serializable id, Serializable keyspace) {
		final Object result = levelTemplate.get(createDbName(keyspace), id);
		return result != null;
	}

	@Override
	public Object get(Serializable id, Serializable keyspace) {
		Object result = levelTemplate.get(createDbName(keyspace), id);
		return result;
	}

	@Override
	public Object delete(Serializable id, Serializable keyspace) {
		final String dbName = createDbName(keyspace);
		Object result = levelTemplate.get(dbName, id);
		levelTemplate.delete(dbName, id);
		return result;
	}

	@Override
	public Iterable<?> getAllOf(Serializable keyspace) {
		final String dbName = createDbName(keyspace);
		return levelTemplate.getAll(dbName);
	}

	@Override
	public CloseableIterator<Entry<Serializable, Object>> entries(Serializable keyspace) {
		final String dbName = createDbName(keyspace);
		return levelTemplate.execute(dbName, new LevelCallback<CloseableIterator<Entry<Serializable, Object>>>() {

			@SuppressWarnings("rawtypes")
			@Override
			public CloseableIterator<Entry<Serializable, Object>> doInLevel(DB db, LevelSerializer keySerializer, LevelSerializer valueSerializer) throws DataAccessException {
				final DBIterator iterator = db.iterator();
				return new CloseableIterator<Map.Entry<Serializable, Object>>() {

					@Override
					public Entry<Serializable, Object> next() {
						iterator.next();
						return new Entry<Serializable, Object>() {

							@SuppressWarnings("unchecked")
							@Override
							public Object setValue(Object value) {
								return iterator.peekNext().setValue(valueSerializer.serialize(value));
							}

							@Override
							public Object getValue() {
								return valueSerializer.deserialize(iterator.peekNext().getValue());
							}

							@Override
							public Serializable getKey() {
								return (Serializable) keySerializer.deserialize(iterator.peekNext().getKey());
							}
						};
					}

					@Override
					public boolean hasNext() {
						return iterator.hasNext();
					}

					@Override
					public void close() {
						try {
							iterator.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				};
			}
		});
	}

	@Override
	public void deleteAllOf(Serializable keyspace) {
		final String dbName = createDbName(keyspace);
		levelTemplate.delete(dbName);
	}

	@Override
	public void clear() {
		logger.info("clear leveldb cache");
		if (this.autoDestroy)
			this.levelTemplate.destroy(this.cachedb);
	}

	@Override
	public long count(Serializable keyspace) {
		final String dbName = createDbName(keyspace);
		return this.levelTemplate.keys(dbName).size();
	}

	@Override
	public void destroy() throws Exception {
		this.levelTemplate.destroy(this.cachedb);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(levelTemplate, "Source must not be null!");
		Assert.notNull(option.getCache(), "Source must not be null!");
		this.autoDestroy = option.getAutoDestroy();
		this.cachedb = option.getCache();
	}

	protected String createDbName(Serializable keyspace) {
		// byte[] bytes = toBytes(keyspace);
		// return CACHE_PIX + HashCode.fromBytes(bytes).asLong();
		return cachedb;
	}

	// public byte[] createKey(String keyspace, String id) {
	// return toBytes(keyspace + ":" + id);
	// }

	/**
	 * Convert given source to binary representation using the underlying
	 * {@link ConversionService}.
	 *
	 * @param source
	 * @return
	 * @throws ConverterNotFoundException
	 */
	// public byte[] toBytes(Object source) {
	// if (source instanceof byte[]) {
	// return (byte[]) source;
	// }
	// return JSON.toJSONBytes(source);
	// // return conversionService.convert(source, byte[].class);
	// }

}
