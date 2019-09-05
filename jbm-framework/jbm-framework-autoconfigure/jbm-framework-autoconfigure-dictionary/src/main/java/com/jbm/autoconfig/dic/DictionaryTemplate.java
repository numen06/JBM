package com.jbm.autoconfig.dic;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jbm.framework.dictionary.JbmDictionary;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

/**
 * 业务字典模板
 *
 * @author wesley.zhang
 */
@Slf4j
public class DictionaryTemplate {
    private final static Logger logger = LoggerFactory.getLogger(DictionaryTemplate.class);
    @Value("${spring.application.name:}")
    private String application;

    public String getApplication() {
        return application;
    }

    private Map<String, List<JbmDictionary>> jbmDicMapCache = Maps.newLinkedHashMap();

    public DictionaryTemplate() {
        super();
    }


    /**
     * 获取所有字典
     *
     * @return
     */
    public List<JbmDictionary> getAllDictionarys() {
        List<JbmDictionary> jbmDictionaries = Lists.newArrayList();
        for (String key : jbmDicMapCache.keySet()) {
            jbmDictionaries.addAll(jbmDicMapCache.get(key));
        }
        return jbmDictionaries;
    }

    /**
     * 获取对应的字典缓存
     *
     * @return
     */
    public Map<String, List<JbmDictionary>> getJbmDicMapCache() {
        return jbmDicMapCache;
    }

    /**
     * 通过枚举类型获取字典
     *
     * @param clazz
     * @return
     */
    public List<JbmDictionary> getValues(Class<? extends Enum<?>> clazz) {
        return getValues(clazz.getSimpleName());
    }

    /**
     * 通过类型获取字典
     *
     * @param type
     * @return
     */
    public List<JbmDictionary> getValues(String type) {
        try {
            return jbmDicMapCache.get(type);
        } catch (Exception e) {
            logger.error("读取缓存失败", e);
        }
        return null;
    }


    /**
     * 插入到字典
     *
     * @param jbmDictionaries
     */
    public void putIfAbsent(List<JbmDictionary> jbmDictionaries) {
        for (JbmDictionary jbmDictionary : jbmDictionaries) {
            this.putIfAbsent(jbmDictionary);
        }
    }

    /**
     * 插入字典
     *
     * @param jbmDictionary
     */
    public void putIfAbsent(JbmDictionary jbmDictionary) {
        if (!this.jbmDicMapCache.containsKey(jbmDictionary.getType())) {
            this.jbmDicMapCache.putIfAbsent(jbmDictionary.getType(), Lists.newArrayList());
        }
        jbmDictionary.setApplication(application);
        jbmDicMapCache.get(jbmDictionary.getType()).add(jbmDictionary);
    }
}
