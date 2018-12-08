package com.jbm.autoconfig.dic;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.util.AnnotatedTypeScanner;

import com.alibaba.fastjson.JSON;
import com.jbm.framework.constant.Constant;
import com.jbm.framework.constant.ConstantDictionary;

/**
 * 字典扫描类</br>
 * 发现并注册字典
 * 
 * @author wesley.zhang
 * @date 2018年11月29日 下午4:43:26
 */
public class DictionaryScanner {
	private final static Logger logger = LoggerFactory.getLogger(DictionaryScanner.class);

	private ApplicationContext applicationContext;

	private final String basepackage;

	private DictionaryTemplate dictionaryCache;

	private Map<Class<?>, Constant> classCache = new ConcurrentHashMap<>();

	public DictionaryScanner(ApplicationContext applicationContext, DictionaryTemplate dictionaryCache,
			String basepackage) {
		super();
		this.applicationContext = applicationContext;
		this.basepackage = basepackage;
		this.dictionaryCache = dictionaryCache;
	}

	private synchronized Constant findConstant(Class<?> clazz) {
		Constant ann = classCache.get(clazz);
		if (ann != null)
			return ann;
		ann = AnnotationUtils.findAnnotation(clazz, Constant.class);
		classCache.put(clazz, ann);
		return ann;
	}

	private void putIfAbsent(String namespaces, String key, String value) {
		// dictionaryCache.getValues(namespaces).put(key, value);
		DictionaryCacheBean bean = new DictionaryCacheBean();
		bean.setNamespaces(namespaces);
		bean = dictionaryCache.getDictionaryCacheBean(bean);
		bean.put(key, value);
		dictionaryCache.putIfAbsent(bean);
		logger.info("put system cache namespaces:[{}] key:[{}],value[{}]", namespaces, key, value);
	}

	public void scanner() throws IllegalAccessException {
		// 扫描所有基本类
		AnnotatedTypeScanner scanner = new AnnotatedTypeScanner(true, Constant.class);
		scanner.setEnvironment(applicationContext.getEnvironment());
		scanner.setResourceLoader(applicationContext);
		Set<Class<?>> set = scanner.findTypes(basepackage);
		for (Iterator<Class<?>> iterator = set.iterator(); iterator.hasNext();) {
			Class<?> clz = iterator.next();
			if (clz.isInterface()) {
				Constant ann = findConstant(clz);
				String namespaces = ann.namespaces();
				if (StringUtils.isBlank(namespaces)) {
					namespaces = clz.getName();
				}
				final Map<String, Object> tempCache = new HashMap<String, Object>();
				for (Field field : clz.getFields()) {
					Class<?> typeClass = field.getType();
					if (typeClass.equals(ConstantDictionary.class)) {
						final ConstantDictionary val = (ConstantDictionary) FieldUtils.readStaticField(field);
						if (tempCache.containsKey(val.getCode())) {
							throw new RuntimeException(MessageFormat.format("已经存在相通的Code:{0},请检查静态字典", val.getCode()));
						}
						tempCache.put(val.getCode(), null);
						putIfAbsent(namespaces, field.getName(), JSON.toJSONString(val));
					}
				}
			}
		}
	}

}
