package com.jbm.framework.masterdata.code.generate;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.usage.entity.MasterDataTreeEntity;
import com.jbm.framework.masterdata.usage.entity.MultiPlatformTreeEntity;
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
        Class<?> entityClass = generateSource.getEntityClass();
        if (MultiPlatformTreeEntity.class.isAssignableFrom(entityClass)) {
            return "controllerMultiPlatformTree";
        }
        if (MasterDataTreeEntity.class.isAssignableFrom(entityClass)) {
            return "controllerTree";
        }
        return "controller";
    }

    @SneakyThrows
    public String getSuperClass(GenerateSource generateSource) {
        String extClass = "com.jbm.framework.mvc.web.BaseController";
        generateSource.getData().put("extClass", extClass);
        generateSource.getData().put("extClassName", StrUtil.subAfter(extClass, ".", true));
        return extClass;
    }


    @Override
    public CodeType getCodeType() {
        return CodeType.controller;
    }
}
