package com.jbm.autoconfig.dic;

import com.jbm.framework.dictionary.JbmDictionary;

import java.util.List;

/**
 * 字典类型转换
 *
 * @param <T>
 * @author wesley
 */
public interface ITypeConverter<T> {
    List<JbmDictionary> convert(T obj);
}
