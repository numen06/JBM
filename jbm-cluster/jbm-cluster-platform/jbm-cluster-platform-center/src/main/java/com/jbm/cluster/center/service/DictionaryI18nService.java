package com.jbm.cluster.center.service;

import com.jbm.cluster.api.entitys.basic.BaseDic;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 字典国际化。未配置国际化文案的字典保持原名称。
 */
public interface DictionaryI18nService {

    Map<String, List<BaseDic>> localize(Map<String, List<BaseDic>> dictionaries, Locale locale);
}
