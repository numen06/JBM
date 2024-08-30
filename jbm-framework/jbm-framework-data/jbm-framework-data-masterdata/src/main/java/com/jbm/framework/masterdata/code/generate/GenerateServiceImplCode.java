package com.jbm.framework.masterdata.code.generate;

import cn.hutool.core.util.StrUtil;
import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.usage.entity.*;
import com.jbm.util.StringUtils;
import lombok.SneakyThrows;

public class GenerateServiceImplCode extends BaseGenerateCodeImpl {


    @SneakyThrows
    public String getSuperClass(GenerateSource generateSource) {
        String extClass = null;
        Class<?> superclass = generateSource.getSuperclass();
        while (true) {
            if (superclass.equals(MasterDataEntity.class)) {
                extClass = "com.jbm.framework.service.mybatis.MasterDataServiceImpl";
            }
            if (superclass.equals(MasterDataIdEntity.class)) {
                extClass = "com.jbm.framework.service.mybatis.MasterDataServiceImpl";
            }
            if (superclass.equals(MasterDataCodeEntity.class)) {
                extClass = "com.jbm.framework.service.mybatis.MasterDataServiceImpl";
            }
            if (superclass.equals(MasterDataTreeEntity.class)) {
                extClass = "com.jbm.framework.service.mybatis.MasterDataTreeServiceImpl";
            }
            if (superclass.equals(MultiPlatformEntity.class)) {
                extClass = "com.jbm.framework.service.mybatis.MultiPlatformServiceImpl";
            }
            if (superclass.equals(MultiPlatformIdEntity.class)) {
                extClass = "com.jbm.framework.service.mybatis.MultiPlatformServiceImpl";
            }
            if (superclass.equals(MultiPlatformTreeEntity.class)) {
                extClass = "com.jbm.framework.service.mybatis.MultiPlatformTreeServiceImpl";
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
        return CodeType.serviceImpl;
    }
}
