package com.jbm.autoconfig.dic;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.EnumUtil;
import com.jbm.util.ListUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

/**
 * 字典扫描类</br>
 * 发现并注册字典
 *
 * @author wesley.zhang
 * @date 2018年11月29日 下午4:43:26
 */
@Slf4j
public class DictionaryScanner {


    private final EnumScanPackages enumScanPackages;

    private final DictionaryTemplate dictionaryTemplate;

    private List<JbmDictionary> jbmDictionaryArrayList = ListUtils.newArrayList();

    private ITypeConverter typeConverter = new EnumTypeConverter();


    public DictionaryScanner(DictionaryTemplate dictionaryTemplate, EnumScanPackages enumScanPackages) {
        super();
        this.enumScanPackages = enumScanPackages;
        this.dictionaryTemplate = dictionaryTemplate;
    }

    private void putIfAbsent(List<JbmDictionary> jbmDictionaries) {
        for (JbmDictionary jbmDictionary : jbmDictionaries) {
            log.info("put [{}] cache type:[{}] code:[{}],value[{}]", jbmDictionary.getApplication(), jbmDictionary.getType(), jbmDictionary.getCode(), jbmDictionary.getValue());
            dictionaryTemplate.putIfAbsent(jbmDictionary);
        }
        jbmDictionaryArrayList.addAll(jbmDictionaries);
    }

    public List<JbmDictionary> scanner() {
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
        return this.jbmDictionaryArrayList;
    }

}
