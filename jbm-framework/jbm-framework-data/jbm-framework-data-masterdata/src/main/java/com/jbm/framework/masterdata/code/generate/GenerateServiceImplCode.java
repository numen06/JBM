package com.jbm.framework.masterdata.code.generate;

import cn.hutool.core.util.StrUtil;
import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.util.StringUtils;
import lombok.SneakyThrows;

public class GenerateServiceImplCode extends BaseGenerateCodeImpl {


    @SneakyThrows
    public String getSuperClass(GenerateSource generateSource) {
        String extClass = null;
        Class<?> superclass = generateSource.getSuperclass();
        while (true) {
            if (superclass.equals(MasterDataEntity.class)) {
                if (hasTreeFields(generateSource.getEntityClass())) {
                    extClass = "com.jbm.framework.service.mybatis.MasterDataTreeServiceImpl";
                } else {
                    extClass = "com.jbm.framework.service.mybatis.MasterDataServiceImpl";
                }
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

    private boolean hasTreeFields(Class<?> entityClass) {
        try {
            entityClass.getDeclaredField("parentId");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    @Override
    public CodeType getCodeType() {
        return CodeType.serviceImpl;
    }
}
