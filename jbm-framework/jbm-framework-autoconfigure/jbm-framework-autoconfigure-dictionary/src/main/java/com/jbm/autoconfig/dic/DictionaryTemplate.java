package com.jbm.autoconfig.dic;

import com.google.common.collect.Lists;
import com.jbm.framework.dictionary.JbmDictionary;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private DictionaryScanner dictionaryScanner;

    public DictionaryTemplate(DictionaryScanner dictionaryScanner) {
        this.dictionaryScanner = dictionaryScanner;
    }

    public DictionaryTemplate() {
        super();
    }

    public String getApplication() {
        return dictionaryScanner.getApplication();
    }

    /**
     * 获取所有字典
     *
     * @return
     */
    public List<JbmDictionary> getAllDictionarys() {
        List<JbmDictionary> jbmDictionaries = Lists.newArrayList();
        for (String key : dictionaryScanner.getJbmDicMapCache().keySet()) {
            jbmDictionaries.addAll(dictionaryScanner.getJbmDicMapCache().get(key));
        }
        return jbmDictionaries;
    }

    /**
     * 获取对应的字典缓存
     *
     * @return
     */
    public Map<String, List<JbmDictionary>> getJbmDicMapCache() {
        return dictionaryScanner.getJbmDicMapCache();
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
            return dictionaryScanner.getJbmDicMapCache().get(type);
        } catch (Exception e) {
            log.error("读取缓存失败", e);
        }
        return null;
    }


}
