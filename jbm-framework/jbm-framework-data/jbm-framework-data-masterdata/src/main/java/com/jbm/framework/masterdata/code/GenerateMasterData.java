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
import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.generate.*;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.util.template.simple.SimpleTemplateEngine;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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

    private String mapperModule;

    private String mapperXmlModule;

    private String serviceImplModule;

    private String controllerModule;

    private String businessModule;

    private String businessImplModule;

    private String mapperPackage;

    private String servicePackage;

    private String controllerPackage;

    private String businessPackage;

    /** 是否生成 Mapper 模块（mapper + mapperXml），默认 true */
    private boolean enableMapper = true;
    /** 是否生成 Service 模块（service + serviceImpl），默认 true */
    private boolean enableService = true;
    /** 是否生成 Controller 模块，默认 true */
    private boolean enableController = true;
    /** 是否生成 Business 模块（business + businessImpl），默认 true */
    private boolean enableBusiness = true;

    private final List<IGenerateCode> generateCodeList = new ArrayList<>();

    private final static String MASTERCARD_TEMP_PATH = "com/jbm/framework/masterdata/code/btl/";

    public GenerateMasterData() {
        try {
            //构建模板引擎
            TemplateConfig templateConfig = new TemplateConfig(MASTERCARD_TEMP_PATH, TemplateConfig.ResourceMode.CLASSPATH);
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

    private final Map<Class<?>, List<Class<?>>> businessGroups = Maps.newConcurrentMap();

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
                generateSource.setMapperModule(StrUtil.trimToNull(mapperModule));
                generateSource.setMapperXmlModule(StrUtil.trimToNull(mapperXmlModule));
                generateSource.setServiceImplModule(StrUtil.trimToNull(serviceImplModule));
                generateSource.setControllerModule(StrUtil.trimToNull(controllerModule));
                generateSource.setBusinessModule(StrUtil.trimToNull(businessModule));
                generateSource.setBusinessImplModule(StrUtil.trimToNull(businessImplModule));
                generateSource.setMapperPackage(StrUtil.trimToNull(mapperPackage));
                generateSource.setServicePackage(StrUtil.trimToNull(servicePackage));
                generateSource.setControllerPackage(StrUtil.trimToNull(controllerPackage));
                generateSource.setBusinessPackage(StrUtil.trimToNull(businessPackage));
                generateSourceList.add(generateSource);
            }
        });
        return generateSourceList;
    }

    public GenerateSource generate(GenerateSource generateSource) throws Exception {
        for (IGenerateCode iGenerateCode : this.generateCodeList) {
            if (!isModuleEnabled(iGenerateCode.getCodeType())) {
                log.debug("跳过[{}]生成: 模块已禁用", iGenerateCode.getCodeType().name());
                continue;
            }
            try {
                Template template = templateEngine.getTemplate(iGenerateCode.getTemplateName(generateSource) + ".btl");
                generateSource.setTemplate(template);
                if (generateSource.getBussinessGroup() != null) {
                    // 将业务范围加入模板（使用 Map 列表避免模板引擎对 Class 反射，兼容 Java 9+ 模块）
                    List<Class<?>> entityClasses = businessGroups.get(generateSource.getBussinessGroup().businessClass());
                    List<Map<String, String>> bussinessEntityList = new ArrayList<>();
                    if (entityClasses != null) {
                        for (Class<?> c : entityClasses) {
                            Map<String, String> m = new HashMap<>();
                            m.put("simpleName", c.getSimpleName());
                            m.put("name", c.getName());
                            bussinessEntityList.add(m);
                        }
                    }
                    generateSource.getData().put("bussinessEntityList", bussinessEntityList);
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

    /**
     * 按代码类型判断所属模块是否启用。mapper/mapperXml 属 mapper 模块，service/serviceImpl 属 service 模块，controller 属 controller 模块，business/businessImpl 属 business 模块。
     */
    private boolean isModuleEnabled(CodeType codeType) {
        if (codeType == null) return false;
        switch (codeType) {
            case mapper:
            case mapperXml:
                return enableMapper;
            case service:
            case serviceImpl:
                return enableService;
            case controller:
                return enableController;
            case business:
            case businessImpl:
                return enableBusiness;
            default:
                return true;
        }
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
