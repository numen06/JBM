package com.jbm.framework.masterdata.code;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jbm.framework.masterdata.code.annotation.BussinessGroup;
import com.jbm.framework.masterdata.code.annotation.IgnoreGeneate;
import com.jbm.framework.masterdata.code.generate.*;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.util.template.simple.SimpleTemplateEngine;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


/**
 * @author: create by wesley
 * @date:2019/4/18
 */
@Slf4j
@Data
public class GenerateMasterData {

//    private GroupTemplate groupTemplate;

    private TemplateEngine templateEngine;

    private String targetPackage;

    private String serviceModule;

    private String daoModule;

    private final List<IGenerateCode> generateCodeList = new ArrayList<>();

    private final static String MASTERDATA_TEMP_PATH = "com/jbm/framework/masterdata/code/btl/";

    public GenerateMasterData() {
        try {
            //构建模板引擎
            TemplateConfig templateConfig = new TemplateConfig(MASTERDATA_TEMP_PATH, TemplateConfig.ResourceMode.CLASSPATH);
            templateConfig = templateConfig.setCustomEngine(SimpleTemplateEngine.class);
            templateEngine = TemplateUtil.createEngine(templateConfig);

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

    private final Map<Class, List<Class>> businessGroups = Maps.newConcurrentMap();

    public List<GenerateSource> filter(Set<Class<?>> entitys) throws Exception {
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
                generateSource.setServiceModule(StrUtil.trimToNull(serviceModule));
                generateSource.setDaoModule(StrUtil.trimToNull(daoModule));
                generateSourceList.add(generateSource);
            }
        });
        return generateSourceList;
    }

    public GenerateSource generate(GenerateSource generateSource) throws Exception {
        for (IGenerateCode iGenerateCode : this.generateCodeList) {
//            generateSource = this.buildSource(entityClass, targetPackage);
            try {
                Template template = templateEngine.getTemplate(iGenerateCode.getTemplateName(generateSource) + ".btl");
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
                template.render(generateSource.getData(), file);
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
