package com.jbm.autoconfig.dic;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.util.AnnotatedTypeScanner;

import com.alibaba.fastjson.JSON;
import com.jbm.framework.ann.dic.Constant;

public class DictionaryScanner {
	private final static Logger logger = LoggerFactory.getLogger(DictionaryScanner.class);

	private ApplicationContext applicationContext;

	private final String basepackage;

	private DictionaryCache dictionaryCache;

	public DictionaryScanner(ApplicationContext applicationContext, DictionaryCache dictionaryCache, String basepackage) {
		super();
		this.applicationContext = applicationContext;
		this.basepackage = basepackage;
		this.dictionaryCache = dictionaryCache;
	}

	private void putIfAbsent(String namespaces, String key, String value) {
		// dictionaryCache.getValues(namespaces).put(key, value);
		DictionaryCacheBean bean = new DictionaryCacheBean();
		bean.setNamespaces(namespaces);
		bean = dictionaryCache.getDictionaryCacheBean(bean);
		bean.put(key, value);
		dictionaryCache.putIfAbsent(bean);
		logger.debug("put system cache map:[{}] key:[{}],value[{}]", namespaces, key, value);
	}

	@SuppressWarnings("unchecked")
	public void scanner() {
		// 扫描所有基本类
		AnnotatedTypeScanner scanner = new AnnotatedTypeScanner(Constant.class);
		scanner.setEnvironment(applicationContext.getEnvironment());
		scanner.setResourceLoader(applicationContext);
		Set<Class<?>> set = scanner.findTypes(basepackage);
		for (Iterator<Class<?>> iterator = set.iterator(); iterator.hasNext();) {
			Class<?> clz = iterator.next();
			if (clz.isEnum()) {
				// Constant ann = AnnotationUtils.findAnnotation(clz,
				// Constant.class);
				// for (Object obj : clz.getEnumConstants()) {
				// putIfAbsent(dictionaryCache, ann.namespaces(),
				// obj.toString(), "");
				// }
			} else if (clz.isInterface()) {
				Constant ann = AnnotationUtils.findAnnotation(clz, Constant.class);
				for (Field field : clz.getFields()) {
					try {
						String namespaces = ann.namespaces();
						if (ann.clazz() != null && ann.clazz() != Constant.class) {
							namespaces = ann.clazz().getName();
						}
						putIfAbsent(namespaces, field.getName(), JSON.toJSONString(field.get(null)));
					} catch (Exception e) {
						// e.printStackTrace();
					}
				}
			} else {
				Field[] fields = clz.getDeclaredFields();
				Constant ann = AnnotationUtils.findAnnotation(clz, Constant.class);
				for (Field field : fields) {
					// 静态文件
					String descriptor = Modifier.toString(field.getModifiers());// 获得其属性的修饰
					try {
						if (descriptor != null && descriptor.indexOf("final") > 1 && descriptor.indexOf("static") > 1) {
							// this.getValues(getMapName(ann.namespaces())).putIfAbsent(field.getName(),
							// JSON.toJSONString(field.get(null)));
							putIfAbsent(ann.namespaces(), field.getName(), JSON.toJSONString(field.get(null)));
						}
					} catch (Exception e) {
						logger.error("扫描缓存错误", e);
					}
				}
			}
		}
	}
}
