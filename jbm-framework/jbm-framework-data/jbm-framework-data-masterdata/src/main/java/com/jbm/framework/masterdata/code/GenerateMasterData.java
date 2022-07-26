package com.jbm.framework.masterdata.code;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ClassUtil;
import com.jbm.framework.masterdata.code.annotation.IgnoreGeneate;
import com.jbm.framework.masterdata.code.generate.*;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import lombok.extern.slf4j.Slf4j;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;


/**
 * @author: create by wesley
 * @date:2019/4/18
 */
@Slf4j
public class GenerateMasterData {

    private GroupTemplate groupTemplate;

    private List<IGenerateCode> generateCodeList = new ArrayList<>();

    public GenerateMasterData() {
        try {
            ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader("/com/jbm/framework/masterdata/code/btl");
            Configuration cfg = Configuration.defaultConfiguration();
            groupTemplate = new GroupTemplate(resourceLoader, cfg);
            generateCodeList.add(new GenerateBusinessCode(groupTemplate));
            generateCodeList.add(new GenerateControllerCode(groupTemplate));
            generateCodeList.add(new GenerateMapperCode(groupTemplate));
            generateCodeList.add(new GenerateServiceCode(groupTemplate));
            generateCodeList.add(new GenerateServiceImplCode(groupTemplate));
        } catch (IOException e) {
            log.error("初始化代码构建器失败");
        }
    }

    public static void scanGnerate(String entityPackage, String targetPackage) {
        GenerateMasterData generateMasterData = new GenerateMasterData();
        try {
            Set<Class<?>> entitys = ClassUtil.scanPackage(entityPackage);
            log.info("自动扫描生成代码:{},目标包名:{},发现{}个实体", entityPackage, targetPackage, entitys.size());
            for (Class clazz : entitys) {
                try {
                    generateMasterData.generate(clazz, targetPackage);
                } catch (FileSystemNotFoundException e) {
                    //没找到文件就说明没有在开发环境
                    break;
                } catch (Exception e) {
                    log.error("生成代码错误Class:{}", clazz, e);
                }
            }
        } catch (Exception e) {
            log.error("生成代码错误", e);
        }
    }

    public GenerateSource generate(Class<?> entityClass, String targetPackage) throws Exception {
        GenerateSource generateSource = this.buildSource(entityClass, targetPackage);
        generateCodeList.forEach(new Consumer<IGenerateCode>() {
            @Override
            public void accept(IGenerateCode iGenerateCode) {
                try {
                    Template template = iGenerateCode.createTemplate();
                    generateSource.setTemplate(template);
                    File file = iGenerateCode.generate(generateSource);
//                    generateSource.getTemplate().renderTo(FileUtil.getOutputStream(file));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return generateSource;
    }

    public GenerateSource buildSource(Class<?> entityClass, String targetPackage) {
        GenerateSource generateSource = new GenerateSource();
        generateSource.setEntityClass(entityClass);
        generateSource.setTargetPackage(targetPackage);
        IgnoreGeneate ignoreGeneate = AnnotationUtil.getAnnotation(entityClass, IgnoreGeneate.class);
        generateSource.setIgnoreGeneate(ignoreGeneate);
        ApiModel apiModel = AnnotationUtil.getAnnotation(entityClass, ApiModel.class);
        generateSource.setApiModel(apiModel);
        //        generateSource.setTargetPackage(ClassUtil.getPackage(generateSource.getEntityClass()));
        if (MasterDataEntity.class.isAssignableFrom(generateSource.getEntityClass())) {
            generateSource.setSuperclass(generateSource.getEntityClass().getSuperclass());
        }
        if (generateSource.getSuperclass() == null) {
            log.debug("未检测到超类跳过:{}", entityClass);
        }
        return generateSource;
    }


}
