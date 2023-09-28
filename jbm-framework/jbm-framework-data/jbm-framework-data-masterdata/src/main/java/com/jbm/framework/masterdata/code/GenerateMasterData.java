package com.jbm.framework.masterdata.code;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ClassUtil;
import com.github.pfmiles.minvelocity.TemplateUtil;
import com.github.pfmiles.org.apache.velocity.Template;
import com.github.pfmiles.org.apache.velocity.VelocityContext;
import com.github.pfmiles.org.apache.velocity.context.Context;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jbm.framework.masterdata.code.annotation.BussinessGroup;
import com.jbm.framework.masterdata.code.annotation.IgnoreGeneate;
import com.jbm.framework.masterdata.code.generate.*;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.util.SimpleTemplateUtils;
import io.swagger.annotations.ApiModel;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.FileSystemNotFoundException;
import java.util.*;
import java.util.function.Consumer;


/**
 * @author: create by wesley
 * @date:2019/4/18
 */
@Slf4j
public class GenerateMasterData {

//    private GroupTemplate groupTemplate;

    private List<IGenerateCode> generateCodeList = new ArrayList<>();

    private final static String MASTERDATA_TEMP_PATH =  "com/jbm/framework/masterdata/code/btl/";

    public GenerateMasterData() {
        try {
//            ClassPathResource resourceLoader = new ClassPathResource("/com/jbm/framework/masterdata/code/btl");
//            Configuration cfg = Configuration.defaultConfiguration();
//            groupTemplate = new GroupTemplate(resourceLoader, cfg);
            generateCodeList.add(new GenerateMapperXmlCode());
            generateCodeList.add(new GenerateMapperCode());
            generateCodeList.add(new GenerateServiceCode());
            generateCodeList.add(new GenerateServiceImplCode());
            generateCodeList.add(new GenerateBusinessCode());
            generateCodeList.add(new GenerateBusinessImplCode());
            generateCodeList.add(new GenerateControllerCode());
        } catch (Exception e) {
            log.error("初始化代码构建器失败");
        }
    }

    public static void scanGnerate(String entityPackage, String targetPackage) {
        GenerateMasterData generateMasterData = new GenerateMasterData();
        try {
            Set<Class<?>> entitys = ClassUtil.scanPackage(entityPackage);
            List<GenerateSource> generateSourceList = generateMasterData.filter(entitys, targetPackage);
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

    private Map<Class, List<Class>> businessGroups = Maps.newConcurrentMap();

    public List<GenerateSource> filter(Set<Class<?>> entitys, String targetPackage) throws Exception {
        List<GenerateSource> generateSourceList = new ArrayList<>();

        entitys.forEach(new Consumer<Class<?>>() {
            @Override
            public void accept(Class<?> entity) {
                GenerateSource generateSource = buildSource(entity, targetPackage);
                if (generateSource.getBussinessGroup() != null) {
                    if (businessGroups.containsKey(generateSource.getBussinessGroup().businessClass())) {
                        businessGroups.get(generateSource.getBussinessGroup().businessClass()).add(entity);
                    } else {
                        businessGroups.put(generateSource.getBussinessGroup().businessClass(), Lists.newArrayList(entity));
                    }
                }
                generateSourceList.add(generateSource);
            }
        });
        return generateSourceList;
    }

    public GenerateSource generate(GenerateSource generateSource) throws Exception {
        for (IGenerateCode iGenerateCode : this.generateCodeList) {
//            generateSource = this.buildSource(entityClass, targetPackage);
            try {
                Template template = SimpleTemplateUtils.getTemplate(MASTERDATA_TEMP_PATH + iGenerateCode.getTemplateName(generateSource) + ".btl");
                generateSource.setTemplate(template);
                if (generateSource.getBussinessGroup() != null) {
                    //将业务范围加入模板
                    generateSource.getData().put("bussinessEntityList", businessGroups.get(generateSource.getBussinessGroup().businessClass()));
                }
                try {
                    iGenerateCode.pre(generateSource);
                } catch (Exception e) {
                    log.debug("跳过[{}]生成:[{}],原因:{}", iGenerateCode.getCodeType().name(), generateSource.getEntityClass(), e.getMessage());
                    continue;
                }
                File file = iGenerateCode.generate(generateSource);
                if (MapUtil.isNotEmpty(generateSource.getData())) {
                    generateSource.getData().putAll(generateSource.getData());
                }
                Writer writer  = FileUtil.getWriter(file, CharsetUtil.UTF_8,false);
                try {
                    SimpleTemplateUtils.renderTemplate(generateSource.getTemplate(), generateSource.getData(), writer);
                } finally {
                    writer.close();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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
        BussinessGroup bussinessGroup = AnnotationUtil.getAnnotation(entityClass, BussinessGroup.class);
        generateSource.setBussinessGroup(bussinessGroup);
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
