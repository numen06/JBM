package com.jbm.framework.masterdata.code.generate;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.usage.entity.*;
import com.jbm.util.StringUtils;
import lombok.SneakyThrows;

public class GenerateControllerCode extends BaseGenerateCodeImpl {


    @Override
    public String getCodeFileName(GenerateSource generateSource) {
        CodeType codeType = this.getCodeType();
        String ext = ".java";
        String suffix = StrUtil.upperFirst(codeType.name());
        String fileName = generateSource.getEntityClass().getSimpleName() + suffix + ext;
        if (ObjectUtil.isNotNull(generateSource.getBussinessGroup())) {
            String businessName = GenerateBusinessImplCode.getBusinessName(generateSource);
            fileName = businessName + suffix + ext;
        }
        return fileName;
    }

    @Override
    public String getTemplateName(GenerateSource generateSource) {
        if (ObjectUtil.isNotNull(generateSource.getBussinessGroup())) {
            return "businessController";
        }
        return super.getTemplateName(generateSource);
    }

    @SneakyThrows
    public String getSuperClass(GenerateSource generateSource) {
        String extClass = null;
        Class<?> superclass = generateSource.getSuperclass();
        while (true) {
            if (superclass.equals(MasterDataEntity.class)) {
                extClass = "com.jbm.framework.mvc.web.MasterDataCollection";
            }
            if (superclass.equals(MasterDataIdEntity.class)) {
                extClass = "com.jbm.framework.mvc.web.MasterDataCollection";
            }
            if (superclass.equals(MasterDataCodeEntity.class)) {
                extClass = "com.jbm.framework.mvc.web.MasterDataCollection";
            }
            if (superclass.equals(MasterDataTreeEntity.class)) {
                extClass = "com.jbm.framework.mvc.web.MasterDataTreeCollection";
            }
            if (superclass.equals(MultiPlatformEntity.class)) {
                extClass = "com.jbm.framework.service.mybatis.MultiPlatformCollection";
            }
            if (superclass.equals(MultiPlatformIdEntity.class)) {
                extClass = "com.jbm.framework.service.mybatis.MultiPlatformCollection";
            }
            if (superclass.equals(MultiPlatformTreeEntity.class)) {
                extClass = "com.jbm.framework.service.mybatis.MultiPlatformTreeCollection";
            }
            if (StrUtil.isNotBlank(extClass)) {
                break;
            }
            if (Object.class.equals(superclass)) {
                break;
            }
            superclass = superclass.getSuperclass();
        }
        if (StrUtil.isBlank(extClass)) {
            throw new ClassNotFoundException(StrUtil.format("未发现匹配的父类:{}", generateSource.getEntityClass()));
        }
        generateSource.getData().put("extClass", extClass);
        generateSource.getData().put("extClassName", StringUtils.substringAfterLast(extClass, "."));
        return extClass;
    }


    @Override
    public CodeType getCodeType() {
        return CodeType.controller;
    }
}
