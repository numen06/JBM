package org.springframework.data.level;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.level.serializer.JdkSerializationLevelSerializer;
import org.springframework.data.level.serializer.LevelSerializer;
import org.springframework.util.Assert;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class LevelTemplate<K, V> implements InitializingBean, BeanClassLoaderAware {

	private final static Logger logger = LoggerFactory.getLogger(LevelTemplate.class);
	private final File databaseDir;
	private final DBFactory dBFactory = Iq80DBFactory.factory;

	protected boolean initialized = false;
	private boolean enableDefaultSerializer = true;

	private ClassLoader classLoader;
	private final LevelOption options;

	private LevelSerializer<?> defaultSerializer;
	private LevelSerializer keySerializer = null;
	private LevelSerializer valueSerializer = null;
	private LevelSerializer hashKeySerializer = null;
	private LevelSerializer hashValueSerializer = null;

	private LoadingCache<String, DB> dbCache = CacheBuilder.newBuilder().expireAfterAccess(3, TimeUnit.SECONDS).removalListener(new RemovalListener<String, DB>() {

		@Override
		public void onRemoval(RemovalNotification<String, DB> notification) {
			try {
				notification.getValue().close();
				logger.debug("释放Level DB连接");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}).build(new CacheLoader<String, DB>() {

		@Override
		public DB load(String dbName) throws Exception {
			return getDB(dbName);
		}

		@Override
		public ListenableFuture<DB> reload(String dbName, DB oldValue) throws Exception {
			return Futures.immediateFuture(getDB(dbName));
		}

	});
	// private LevelSerializer<String> stringSerializer = new
	// StringLevelSerializer();

	public LevelTemplate(LevelOption options) {
		super();
		this.databaseDir = new File(options.getRoot());
		logger.info("base level db path :{}", this.databaseDir.toURI());
		this.options = options;
	}

	public DBFactory getDBFactory() {
		return dBFactory;
	}

	public Options getOptions() {
		return options;
	}

	public void put(String dbName, K key, V value) {
		execute(dbName, new LevelCallback<Object>() {

			@Override
			public Object doInLevel(DB db, LevelSerializer keySerializer, LevelSerializer valueSerializer) throws DataAccessException {
				db.put(rawKey(key), rawValue(value));
				return null;
			}
		});
	}

	/**
	 * 
	 * @param dbName
	 * @param map
	 */
	public void putMulti(String dbName, Map<K, V> map) {
		executeBatch(dbName, new LevelBatchCallback<Object>() {
			@Override
			public Object doInLevel(WriteBatch batch) throws DataAccessException {
				for (K key : map.keySet()) {
					V value = map.get(key);
					batch.put(rawKey(key), rawValue(value));
				}
				return null;
			}
		});
	}

	/**
	 * 删除KEY
	 * 
	 * @param dbName
	 * @param key
	 */
	public void delete(String dbName, K key) {
		execute(dbName, new LevelCallback<Object>() {
			@Override
			public Object doInLevel(DB db, LevelSerializer keySerializer, LevelSerializer valueSerializer) throws DataAccessException {
				db.delete(rawKey(key));
				return null;
			}
		});
	}

	/**
	 * 删除表
	 * 
	 * @param dbName
	 */
	public void delete(String dbName) {
		execute(dbName, new LevelCallback<Object>() {
			@Override
			public Object doInLevel(DB db, LevelSerializer keySerializer, LevelSerializer valueSerializer) throws DataAccessException {
				db.forEach(new Consumer<Map.Entry<byte[], byte[]>>() {

					@Override
					public void accept(Entry<byte[], byte[]> t) {
						db.delete(t.getKey());
					}
				});
				return null;
			}
		});
	}

	/**
	 * 销毁数据库
	 * 
	 * @param dbName
	 */
	public void destroy(String dbName) {
		execute(dbName, new LevelCallback<V>() {
			@Override
			public V doInLevel(DB db, LevelSerializer keySerializer, LevelSerializer valueSerializer) throws DataAccessException {
				synchronized (db) {
					try {
						File rc = new File(databaseDir, dbName);
						db.close();
						getDBFactory().destroy(rc, options);
					} catch (IOException e) {
						logger.error("关闭数据库连接错误", e);
					} finally {
						// 释放缓存连接
						dbCache.invalidate(dbName);
					}
				}
				return null;
			}
		});

	}

	public V get(String dbName, K key) {
		return execute(dbName, new LevelCallback<V>() {
			@Override
			public V doInLevel(DB db, LevelSerializer keySerializer, LevelSerializer valueSerializer) throws DataAccessException {
				return (V) deserializeValue(db.get(rawKey(key)));
			}
		});
	}

	public Set<V> getAll(String dbName) {
		return execute(dbName, new LevelCallback<Set<V>>() {
			@Override
			public Set<V> doInLevel(DB db, LevelSerializer keySerializer, LevelSerializer valueSerializer) throws DataAccessException {
				Set<V> keys = new LinkedHashSet<>();
				DBIterator iterator = db.iterator();
				try {
					for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
						V value = deserializeValue(iterator.peekNext().getValue());
						keys.add(value);
					}
				} finally {
					try {
						iterator.close();
					} catch (IOException e) {
						throw new DataRetrievalFailureException("foreach db error", e);
					}
				}
				return keys;
			}
		});
	}

	/**
	 * 找出表格所有的KEY</br>
	 * 如果表格过大请不要使用这个方法，会导致内存溢出
	 * 
	 * @param dbName
	 * @return
	 */
	public Set<K> keys(String dbName) {
		return execute(dbName, new LevelCallback<Set<K>>() {
			@Override
			public Set<K> doInLevel(DB db, LevelSerializer keySerializer, LevelSerializer valueSerializer) throws DataAccessException {
				Set<K> keys = new LinkedHashSet<>();
				DBIterator iterator = db.iterator();
				try {
					for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
						K key = deserializeKey(iterator.peekNext().getKey());
						keys.add(key);
					}
				} finally {
					try {
						iterator.close();
					} catch (IOException e) {
						throw new DataRetrievalFailureException("foreach db error", e);
					}
				}
				return keys;
			}
		});
	}

	public <T> T executeBatch(String dbName, LevelBatchCallback<T> action) {
		try {
			// final DB db = this.getDB(dbName);
			final DB db = dbCache.get(dbName);
			WriteBatch batch = db.createWriteBatch();
			try {
				T result = action.doInLevel(batch);
				db.write(batch);
				return result;
			} finally {
				batch.close();
				// dbCache.invalidate(dbName);
			}
		} catch (Exception e) {
			throw new DataAccessResourceFailureException("error execute leveldb", e);
		}
	}

	public <T> T execute(String dbName, LevelCallback<T> action) {
		try {
			// final DB db = this.getDB(dbName);
			final DB db = dbCache.get(dbName);
			try {
				return action.doInLevel(db, this.keySerializer, this.valueSerializer);
			} finally {
				// dbCache.invalidate(dbName);
			}
		} catch (Exception e) {
			throw new DataAccessResourceFailureException("error execute leveldb", e);
		}
	}

	private byte[] rawKey(K key) {
		Assert.notNull(key, "non null key required");
		if (keySerializer == null && key instanceof byte[]) {
			return (byte[]) key;
		}
		return keySerializer.serialize(key);
	}

	// private byte[] rawString(String key) {
	// return stringSerializer.serialize(key);
	// }

	private byte[] rawValue(V value) {
		if (valueSerializer == null && value instanceof byte[]) {
			return (byte[]) value;
		}
		return valueSerializer.serialize(value);
	}

	private K deserializeKey(byte[] value) {
		return keySerializer != null ? (K) keySerializer.deserialize(value) : (K) value;
	}

	private V deserializeValue(byte[] value) {
		return valueSerializer != null ? (V) valueSerializer.deserialize(value) : (V) value;
	}

	protected synchronized DB getDB(String dbName) throws IOException {
		// Options options = new Options();
		// options.cacheSize(100 * 1048576); // 100MB cache
		// options.createIfMissing(true);
		File rc = new File(databaseDir, dbName);
		DB db = dBFactory.open(rc, options);
		return db;
	}

	/**
	 * Set the {@link ClassLoader} to be used for the default
	 * {@link JdkSerializationRedisSerializer} in case no other
	 * {@link RedisSerializer} is explicitly set as the default one.
	 *
	 * @param resourceLoader
	 *            can be {@literal null}.
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader
	 * @since 1.7.2
	 */

	public void afterPropertiesSet() {

		boolean defaultUsed = false;

		if (defaultSerializer == null) {

			defaultSerializer = new JdkSerializationLevelSerializer(classLoader != null ? classLoader : this.getClass().getClassLoader());
		}

		if (enableDefaultSerializer) {

			if (keySerializer == null) {
				keySerializer = defaultSerializer;
				defaultUsed = true;
			}
			if (valueSerializer == null) {
				valueSerializer = defaultSerializer;
				defaultUsed = true;
			}
			if (hashKeySerializer == null) {
				hashKeySerializer = defaultSerializer;
				defaultUsed = true;
			}
			if (hashValueSerializer == null) {
				hashValueSerializer = defaultSerializer;
				defaultUsed = true;
			}
		}

		if (enableDefaultSerializer && defaultUsed) {
			Assert.notNull(defaultSerializer, "default serializer null and not all serializers initialized");
		}

		initialized = true;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

}
