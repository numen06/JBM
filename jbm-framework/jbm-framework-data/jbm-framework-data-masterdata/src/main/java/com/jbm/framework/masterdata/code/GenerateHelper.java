package com.jbm.framework.masterdata.code;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Filter;
import cn.hutool.core.util.ClassUtil;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.FileSystemNotFoundException;
import java.util.*;


/**
 * @author wesley
 */
@Slf4j
public class GenerateHelper {
    public static void scanGnerate(String entityPackage, Map<String, Object> attributes) {

        GenerateMasterData generateMasterData = new GenerateMasterData();
        BeanUtil.fillBeanWithMap(attributes, generateMasterData, true);
        try {
            // 获取排除包列表
            Set<String> excludePackages = new HashSet<>();
            if (attributes.get("excludePackages") != null) {
                Object excludeObj = attributes.get("excludePackages");
                if (excludeObj instanceof String[]) {
                    excludePackages.addAll(Arrays.asList((String[]) excludeObj));
                } else if (excludeObj instanceof String) {
                    excludePackages.add((String) excludeObj);
                }
            }

            // 使用过滤器扫描包
            Set<Class<?>> entitys = ClassUtil.scanPackage(entityPackage, clazz -> {
                // 检查类所在的包是否在排除列表中
                String classPackageName = clazz.getPackage().getName();
                for (String excludePackage : excludePackages) {
                    if (classPackageName.startsWith(excludePackage)) {
                        return false; // 排除该类
                    }
                }
                return true; // 保留该类
            });

            //排除的
            List<GenerateSource> generateSourceList = generateMasterData.filter(entitys);
            generateSourceList.forEach(generateSource -> {
                try {
                    generateMasterData.generate(generateSource);
                } catch (FileSystemNotFoundException e) {
                    //没找到文件就说明没有在开发环境
                    return;
                } catch (Exception e) {
                    log.error("生成代码错误Class:{}", generateSource.getEntityClass(), e);
                }
            });
        } catch (Exception e) {
            log.error("生成代码错误", e);
        }
        // TODO: 2022/7/29
//        throw new RuntimeException("测试结束");
    }
}
