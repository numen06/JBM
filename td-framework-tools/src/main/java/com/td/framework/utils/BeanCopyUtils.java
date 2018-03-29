package com.td.framework.utils;

import org.apache.commons.lang.ArrayUtils;

/**
 * 复制对象
 * 
 * @author wesley
 *
 */
public class BeanCopyUtils {
	/**
	 * 复制对象参数忽略空值
	 * 
	 * @param src
	 * @param target
	 */
	public static void copyPropertiesIgnoreNull(Object source, Object target) {
		copyProperties(source, target, true);
	}

	/**
	 * 复制对象参数
	 * 
	 * @param source
	 * @param target
	 */
	public static void copyProperties(Object source, Object target) {
		org.springframework.beans.BeanUtils.copyProperties(source, target);
	}

	/**
	 * 复制对象参数
	 * 
	 * @param source
	 * @param target
	 * @param ignoreNull
	 */
	public static void copyProperties(Object source, Object target, boolean ignoreNull, String... ignoreProperties) {
		if (ignoreNull) {
			String[] nullProperties = com.td.util.BeanUtils.getNullPropertyNames(source);
			ignoreProperties = (String[]) ArrayUtils.addAll(ignoreProperties, nullProperties);
		}
		org.springframework.beans.BeanUtils.copyProperties(source, target, ignoreProperties);
	}
}
