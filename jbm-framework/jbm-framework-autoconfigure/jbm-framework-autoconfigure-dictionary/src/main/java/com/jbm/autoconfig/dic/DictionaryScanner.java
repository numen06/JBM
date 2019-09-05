package com.jbm.autoconfig.dic;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.EnumUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jbm.framework.dictionary.JbmDictionary;
import com.jbm.util.ListUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 字典扫描类</br>
 * 发现并注册字典
 *
 * @author wesley.zhang
 * @date 2018年11月29日 下午4:43:26
 */
@Slf4j
public class DictionaryScanner implements InitializingBean {


    private final EnumScanPackages enumScanPackages;

//    private final DictionaryTemplate dictionaryTemplate;

    private Map<String, List<JbmDictionary>> jbmDicMapCache = Maps.newLinkedHashMap();

    public Map<String, List<JbmDictionary>> getJbmDicMapCache() {
        return jbmDicMapCache;
    }

    private ITypeConverter typeConverter = new EnumTypeConverter();

    @Value("${spring.application.name:}")
    private String application;

    public String getApplication() {
        return application;
    }

    public DictionaryScanner(  EnumScanPackages enumScanPackages) {
        super();
        this.enumScanPackages = enumScanPackages;
//        this.dictionaryTemplate = dictionaryTemplate;
    }

    private void putIfAbsent(List<JbmDictionary> jbmDictionaries) {
        for (JbmDictionary jbmDictionary : jbmDictionaries) {
            jbmDictionary.setApplication(application);
            log.info("put application:[{}] cache type:[{}] typeName:[{}] code:[{}],value[{}]", jbmDictionary.getApplication(), jbmDictionary.getType(),jbmDictionary.getTypeName(), jbmDictionary.getCode(), jbmDictionary.getValue());
            this.putIfAbsent(jbmDictionary);
        }
//        jbmDictionaryArrayList.addAll(jbmDictionaries);
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

    public List<JbmDictionary> scanner() {
        List<JbmDictionary> jbmDictionaryArrayList = ListUtils.newArrayList();
        for (String pack : enumScanPackages.getPackageNames()) {
            Set<Class<?>> classes = ClassUtil.scanPackage(pack, new cn.hutool.core.lang.Filter() {
                @Override
                public boolean accept(Object o) {
                    return EnumUtil.isEnum((Class<?>) o);
                }
            });
            for (Class emClass : classes) {
                this.putIfAbsent(typeConverter.convert(emClass));
            }
        }
        return jbmDictionaryArrayList;
    }

    @Override
    public void afterPropertiesSet()  {
        try {
            log.info("JBM开始扫描字典");
            scanner();
            log.info("JBM结束扫描字典");
        } catch (Exception e) {
            log.error("扫描字典错误", e);
        }
    }
}
