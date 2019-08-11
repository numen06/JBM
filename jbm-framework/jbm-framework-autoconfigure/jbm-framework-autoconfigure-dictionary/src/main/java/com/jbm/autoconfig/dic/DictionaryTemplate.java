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
import java.util.function.Consumer;

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


    public List<JbmDictionary> getValues(Class<? extends Enum<?>> clazz) {
        return getValues(clazz.getSimpleName());
    }

    public List<JbmDictionary> getValues(String type) {
        try {
            return jbmDicMapCache.get(type);
        } catch (Exception e) {
            logger.error("读取缓存失败", e);
        }
        return null;
    }


    public void putIfAbsent(List<JbmDictionary> jbmDictionaries) {
        for (JbmDictionary jbmDictionary : jbmDictionaries) {
            this.putIfAbsent(jbmDictionary);
        }
    }

    public void putIfAbsent(JbmDictionary jbmDictionary) {
        if (!this.jbmDicMapCache.containsKey(jbmDictionary.getType())) {
            this.jbmDicMapCache.putIfAbsent(jbmDictionary.getType(), Lists.newArrayList());
        }
        jbmDictionary.setApplication(application);
        jbmDicMapCache.get(jbmDictionary.getType()).add(jbmDictionary);
    }

    public JbmDictionary getValue(Class<? extends Enum<?>> clazz, String code) {
        List<JbmDictionary> jbmDictionaryLists = this.getValues(clazz);
        for (JbmDictionary jbmDictionary : jbmDictionaryLists) {
            if (jbmDictionary.getCode().equals(code)) {
                return jbmDictionary;
            }
        }
        return null;
    }
}
