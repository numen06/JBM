package com.jbm.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;

import com.google.common.collect.Lists;
import com.jbm.util.list.Closure;
import com.jbm.util.list.Derivative;

/**
 * 
 * 针对Collection接口的工具类
 * 
 * @author Wesley
 * 
 */
public class CollectionUtils extends org.apache.commons.collections.CollectionUtils {
	/**
	 * 将数组添加书Collection
	 * 
	 * @param collection
	 * @param elements
	 */
	@SuppressWarnings("rawtypes")
	public static void addAll(Collection collection, Object[] elements) {
		if (ArrayUtils.isEmpty(elements))
			return;
		org.apache.commons.collections.CollectionUtils.addAll(collection, elements);
	}

	/**
	 * 如果已经存在对象则不再添加
	 * 
	 * @param collection
	 * @param elements
	 */
	public static <T> void uniqAdd(Collection<T> collection, T elements) {
		if (collection == null)
			return;
		if (!collection.contains(elements))
			collection.add(elements);
	}

	/**
	 * 得到第一个返回值
	 * 
	 * @param coll
	 * @param def
	 *            默认值
	 * @return
	 */
	public static <T> T firstResult(Collection<T> coll, T def) {
		if (CollectionUtils.isEmpty(coll)) {
			return def;
		}
		T result = coll.iterator().next();
		if (result == null)
			return def;
		return result;
	}

	/**
	 * 得到第一个返回值
	 * 
	 * @param coll
	 * @return
	 */
	public static <T> T firstResult(Collection<T> coll) {
		return firstResult(coll, null);
	}

	/**
	 * 返回第一非空的对象
	 * 
	 * @param coll
	 * @return
	 */
	public static <T> T firstNonEmptyResult(Collection<T> coll) {
		if (CollectionUtils.isEmpty(coll)) {
			return null;
		}
		for (Iterator<T> iterator = coll.iterator(); iterator.hasNext();) {
			T result = iterator.next();
			if (result != null)
				return result;
		}
		return null;
	}

	/**
	 * 获取长度
	 * 
	 * <pre>
	 * CollectionUtils.length(null) = 0
	 * </pre>
	 * 
	 * @param object
	 * @return
	 */
	public static int length(Object object) {
		if (object == null)
			return 0;
		return CollectionUtils.size(object);
	}

	public static <T, E> List<T> excavate(Collection<E> collection, final String key) {
		final List<T> result = new ArrayList<T>();
		CollectionUtils.foreach(collection, new Closure<E>() {
			@SuppressWarnings("unchecked")
			@Override
			public void execute(E input) {
				try {
					T t = (T) PropertyUtils.getProperty(input, key);
					result.add(t);
				} catch (Exception e) {
				}
			}
		});
		return result;
	}

	public static <T, E> List<Map<String, T>> excavateMap(Collection<E> collection, final String... keys) {
		final List<Map<String, T>> result = new ArrayList<Map<String, T>>();
		CollectionUtils.foreach(collection, new Closure<E>() {
			@SuppressWarnings("unchecked")
			@Override
			public void execute(E input) {
				final Map<String, T> map = new HashMap<String, T>();
				try {
					for (int i = 0; i < keys.length; i++) {
						final String key = keys[i];
						T t = (T) PropertyUtils.getProperty(input, key);
						map.put(key, t);
						result.add(map);
					}
				} catch (Exception e) {
				}
			}
		});
		return result;
	}

	/**
	 * 去除重复，原始数据会被替换
	 * 
	 * [1, 2, 3, 4 , 4 , 3] => [1, 2, 3, 4]
	 * 
	 * @param collection
	 * @return
	 */
	public static <T> Collection<T> unique(Collection<T> collection) {
		Set<T> set = new HashSet<T>(collection);
		collection.clear();
		collection.addAll(set);
		return collection;
	}

	public static <E extends Object> void foreach(Collection<E> collection, Closure<E> closure) {
		if (collection != null && closure != null) {
			for (Iterator<E> it = collection.iterator(); it.hasNext();) {
				closure.execute(it.next());
			}
		}
	}

	/**
	 * 衍生方法：将原有的循环转换成新的循环
	 * 
	 * @param source
	 *            原始对象
	 * @param target
	 *            目标对象
	 * @param derivative
	 *            衍生器
	 * 
	 */
	public static <E extends Object, T extends Object> void derivative(Collection<E> source, Collection<T> target, Derivative<E, T> derivative) {
		if (source != null && derivative != null) {
			synchronized (source) {
				for (Iterator<E> it = source.iterator(); it.hasNext();) {
					target.add(derivative.execute(it.next()));
				}
			}
		}
	}

	public static void main(String[] args) {
		// Map<String, String> map1 = MapUtils.split("t1:1;t2:2");
		// Map<String, String> map2 = MapUtils.split("t1:4;t2:5");
		// @SuppressWarnings("unchecked")
		// List<Map<String, String>> list = Lists.newArrayList(map1, map2);
		// System.out.println(JSON.toJSONString(CollectionUtils.excavateMap(list,
		// "t1")));
		List<Integer> collection = Lists.newArrayList(1, 2, 3);
		CollectionUtils.foreach(collection, new Closure<Integer>() {
			@Override
			public void execute(Integer input) {
				System.out.println(input);
			}
		});
		List<String> targer = Lists.newArrayList();
		CollectionUtils.derivative(collection, targer, new Derivative<Integer, String>() {
			@Override
			public String execute(Integer input) {
				return input.toString();
			}
		});
		System.out.println(StringUtils.toString(targer));

	}
}
