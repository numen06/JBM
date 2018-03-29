package com.td.autoconfig.dic;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.keyvalue.core.KeyValueTemplate;

import com.alibaba.fastjson.JSON;
import com.td.framework.ann.dic.Constant;

public class DictionaryCache {
	private final static Logger logger = LoggerFactory.getLogger(DictionaryCache.class);

	@SuppressWarnings("unused")
	private final String center;
	private final String application;
	private KeyValueTemplate keyValueTemplate;

	// private LoadingCache<String, ConcurrentMap<String, String>>
	// dicLoadingCache = CacheBuilder.newBuilder().expireAfterAccess(1,
	// TimeUnit.SECONDS)
	// .build(new CacheLoader<String, ConcurrentMap<String, String>>() {
	// @Override
	// public ConcurrentMap<String, String> load(String type) throws Exception {
	// // ConcurrentMap<String, String> dictionaryMap =
	// // hazelcastInstance.getMap(type);
	// // return dictionaryMap;
	// return null;
	// }
	// });

	public DictionaryCache(KeyValueTemplate keyValueTemplate, String center, String application) {
		super();
		this.keyValueTemplate = keyValueTemplate;
		this.center = center;
		this.application = application;
	}

	private String getRealNamespaces(String namespaces) {
		// return new
		// StringBuffer().append(center).append("-").append(value).toString();
		if (namespaces == null)
			namespaces = "system";
		String temp = MessageFormat.format("{0}/{1}", application, namespaces);
		return temp;
	}

	public String getValue(String namespaces, String key) {
		return getValues(namespaces).get(key);
	}

	public String getValueByClass(Class<?> clazz, String key) {
		return getValues(clazz.getClass().getName()).get(key);
	}

	public String getValue(String key) {
		return getValue(null, key);
	}

	public String getValueByClass(Class<?> clazz) {
		return getValue(clazz.getClass().getName());
	}

	public Map<String, String> getValues(Class<?> clazz) {
		return getValues(clazz.getClass().getName());
	}

	public Map<String, String> getValues(String type) {
		try {
			DictionaryCacheBean bean = keyValueTemplate.findById(getRealNamespaces(type), DictionaryCacheBean.class);
			if (bean == null)
				return new ConcurrentHashMap<>();
			return bean.getKeyMaps();
		} catch (Exception e) {
			logger.error("读取缓存失败", e);
		}
		return null;
	}

	/**
	 * 获取字典列表
	 * 
	 * @param type
	 * @param converter
	 * @return
	 */
	public <T> List<T> getValueList(String type, ITypeConverter<T> converter) {
		Map<String, String> map = getValues(type);
		List<T> list = new ArrayList<T>();
		for (String key : map.keySet()) {
			T t = converter.convert(key, map.get(key));
			list.add(t);
		}
		return list;
	}

	/**
	 * 通过KEY获取到字符串
	 * 
	 * @param key
	 * @return
	 */
	public <E extends Enum<E>> String getValue(E key) {
		return getObject(key, String.class);
	}

	/**
	 * 通过KEY获取到数字
	 * 
	 * @param key
	 * @return
	 */
	public <E extends Enum<E>> Integer getInteger(E key) {
		return getObject(key, Integer.class);
	}

	/**
	 * 获取到对象
	 * 
	 * @param key
	 * @param classType
	 * @return
	 */
	public <T, E extends Enum<E>> T getObject(E key, Class<T> classType) {
		Constant ann = AnnotationUtils.findAnnotation(key.getClass(), Constant.class);
		String text = getValue(ann.namespaces(), key.name());
		return JSON.parseObject(text, classType);
	}

	/**
	 * 获取到对象
	 * 
	 * @param key
	 * @param classType
	 * @return
	 */
	public <T, E extends Enum<E>> T getObject(E key, ITypeConverter<T> converter) {
		Constant ann = AnnotationUtils.findAnnotation(key.getClass(), Constant.class);
		String text = getValue(ann.namespaces(), key.name());
		return converter.convert(key, text);
	}

	public DictionaryCacheBean getDictionaryCacheBean(DictionaryCacheBean bean) {
		bean.setId(MessageFormat.format("{0}/{1}", application, bean.getNamespaces()));
		DictionaryCacheBean cacheBean = keyValueTemplate.findById(bean.getId(), DictionaryCacheBean.class);
		return cacheBean == null ? bean : cacheBean;
	}

	public DictionaryCacheBean putIfAbsent(DictionaryCacheBean bean) {
		bean.setApplication(application);
		bean.setId(MessageFormat.format("{0}/{1}", application, bean.getNamespaces()));
		DictionaryCacheBean cacheBean = keyValueTemplate.findById(bean.getId(), DictionaryCacheBean.class);
		if (cacheBean == null) {
			keyValueTemplate.insert(bean);
		}
		keyValueTemplate.update(bean);
		return bean;
		// dicLoadingCache.invalidate(bean.getId());
	}

}
