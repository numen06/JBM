package com.td.autoconfig.dic;

/**
 * 字典类型转换
 * 
 * @author wesley
 *
 * @param <T>
 */
public interface ITypeConverter<T> {
	<K, V> T convert(K key, V value);
}
