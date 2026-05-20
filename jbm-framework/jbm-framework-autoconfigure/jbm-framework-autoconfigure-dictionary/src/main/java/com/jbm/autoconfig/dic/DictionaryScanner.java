package com.jbm.autoconfig.dic;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.EnumUtil;
import com.google.common.collect.Lists;
import com.jbm.framework.dictionary.JbmDictionary;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字典扫描类</br>
 * 发现并注册字典
 *
 * @author wesley.zhang
 * @date 2018年11月29日 下午4:43:26
 */
@Slf4j
public class DictionaryScanner implements SmartInitializingSingleton {


    private final EnumScanPackages enumScanPackages;

//    private final DictionaryTemplate dictionaryTemplate;

    @Getter
    private final Map<String, List<JbmDictionary>> jbmDicMapCache = new ConcurrentHashMap<>();
    private final ITypeConverter typeConverter = new EnumTypeConverter();
    @Getter
    @Value("${spring.application.name:}")
    private String application;

    public DictionaryScanner(EnumScanPackages enumScanPackages) {
        super();
        this.enumScanPackages = enumScanPackages;
//        this.dictionaryTemplate = dictionaryTemplate;
    }

    private void putIfAbsent(List<JbmDictionary> jbmDictionaries) {
        for (JbmDictionary jbmDictionary : jbmDictionaries) {
            jbmDictionary.setApplication(application);
            log.debug("put application:[{}] cache type:[{}] typeName:[{}] code:[{}],value[{}]", jbmDictionary.getApplication(), jbmDictionary.getType(), jbmDictionary.getTypeName(), jbmDictionary.getCode(), jbmDictionary.getValue());
            this.putIfAbsent(jbmDictionary);
        }
    }

    /**
     * 插入字典
     *
     * @param jbmDictionary
     */
    public void putIfAbsent(JbmDictionary jbmDictionary) {
        jbmDictionary.setApplication(application);
        jbmDicMapCache.computeIfAbsent(jbmDictionary.getType(), k -> Collections.synchronizedList(Lists.newArrayList())).add(jbmDictionary);
    }

    public void scanner() {
        List<String> packageNames = enumScanPackages.getPackageNames();
        if (packageNames.isEmpty()) {
            log.info("JBM字典扫描：未配置扫描包");
            return;
        }

        log.info("JBM开始扫描字典，扫描包数量: {}", packageNames.size());
        long startTime = System.currentTimeMillis();

        for (String pack : packageNames) {
            try {
                Set<Class<?>> classes = ClassUtil.scanPackage(pack, o -> EnumUtil.isEnum((Class<?>) o));
                for (Class<?> emClass : classes) {
                    List<JbmDictionary> dictionaries = typeConverter.convert(emClass);
                    if (dictionaries != null && !dictionaries.isEmpty()) {
                        this.putIfAbsent(dictionaries);
                    }
                }
            } catch (Exception e) {
                log.error("扫描包 [{}] 时发生错误", pack, e);
            }
        }

        long endTime = System.currentTimeMillis();
        int totalTypes = jbmDicMapCache.size();
        int totalItems = jbmDicMapCache.values().stream().mapToInt(List::size).sum();
        log.info("JBM结束扫描字典，耗时: {}ms, 字典类型数: {}, 字典项总数: {}", 
                (endTime - startTime), totalTypes, totalItems);
    }

    @Override
    public void afterSingletonsInstantiated() {
        // 异步执行扫描，不阻塞应用启动
        CompletableFuture.runAsync(() -> {
            try {
                scanner();
            } catch (Exception e) {
                log.error("扫描字典错误", e);
            }
        });
    }
}
