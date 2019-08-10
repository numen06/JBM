package com.jbm.autoconfig.dic;

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
