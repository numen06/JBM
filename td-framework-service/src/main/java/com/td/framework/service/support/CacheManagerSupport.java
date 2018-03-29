package com.td.framework.service.support;

import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;

/**
 * 缓存支持类
 * 
 * @author wesley
 *
 */
public class CacheManagerSupport {

	private CacheManager cacheManager;

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	@SuppressWarnings("unchecked")
	public <T> T getCacheValue(String namespace, Object key) {
		Cache cache = cacheManager.getCache(namespace);
		ValueWrapper value = cache.get(key);
		if (value == null)
			return null;
		return (T) value.get();
	}

}
