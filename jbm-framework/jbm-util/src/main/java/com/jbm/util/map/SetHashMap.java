package com.jbm.util.map;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * <p>
 * Set结构的Map,除了原有的HashMap的功能</br>
 * 会保存一个值是Set结构 依赖于Guava
 * 
 * </p>
 * 
 * @author wesley
 *
 * @param <K>
 * @param <V>
 */
public class SetHashMap<K, V> extends HashMap<K, V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final HashMap<K, Set<V>> listMap = new HashMap<K, Set<V>>();

	@Override
	public V get(Object key) {
		return super.get(key);
	}

	public HashMap<K, Set<V>> toSetMap() {
		return this.listMap;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V put(K key, V value) {
		V v = super.put(key, value);
		if (value instanceof Collection) {
			this.getListToEmpty(key).addAll((Collection<V>) value);
		} else {
			this.getListToEmpty(key).addAll(Sets.newHashSet(value));
		}
		return v;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	@Override
	public V remove(Object key) {
		V v = super.remove(key);
		listMap.remove(key);
		return v;
	}

	public Set<V> getList(K key) {
		return listMap.get(key);
	}

	public Set<V> getListToEmpty(K key) {
		if (!listMap.containsKey(key))
			listMap.put(key, new HashSet<V>());
		return listMap.get(key);
	}

}
