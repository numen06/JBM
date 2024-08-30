package com.jbm.framework.masterdata.code.generate;

import cn.hutool.core.util.StrUtil;
import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.service.IMultiPlatformService;
import com.jbm.framework.masterdata.usage.entity.*;
import lombok.SneakyThrows;

public class GenerateServiceCode extends BaseGenerateCodeImpl {

    @SneakyThrows
    public String getSuperClass(GenerateSource generateSource) {
        String extClass = null;
        Class<?> superclass = generateSource.getSuperclass();
        while (true) {
            if (superclass.equals(MasterDataEntity.class)) {
                extClass = IMasterDataService.class.getName();
            }
            if (superclass.equals(MasterDataIdEntity.class)) {
                extClass = IMasterDataService.class.getName();
            }
            if (superclass.equals(MasterDataCodeEntity.class)) {
                extClass = IMasterDataService.class.getName();
            }
            if (superclass.equals(MasterDataTreeEntity.class)) {
                extClass = IMasterDataTreeService.class.getName();
            }
            if (superclass.equals(MultiPlatformEntity.class)) {
                extClass = IMultiPlatformService.class.getName();
            }
            if (superclass.equals(MultiPlatformIdEntity.class)) {
                extClass = IMultiPlatformService.class.getName();
            }
            if (superclass.equals(MultiPlatformTreeEntity.class)) {
                extClass = IMultiPlatformService.class.getName();
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
        generateSource.getData().put("extClassName", StrUtil.subAfter(extClass, ".", true));
        return extClass;
    }


    @Override
    public CodeType getCodeType() {
        return CodeType.service;
    }
}
