package com.jbm.framework.masterdata.code;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ClassUtil;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.FileSystemNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author wesley
 */
@Slf4j
public class GenerateHelper {
    public static void scanGnerate(String entityPackage, Map<String, Object> attributes) {

        GenerateMasterData generateMasterData = new GenerateMasterData();
        BeanUtil.fillBeanWithMap(attributes, generateMasterData, true);
        try {
            Set<Class<?>> entitys = ClassUtil.scanPackage(entityPackage);
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
