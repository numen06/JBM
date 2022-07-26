package com.jbm.framework.masterdata.code.generate;

import cn.hutool.core.util.StrUtil;
import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.usage.entity.*;
import com.jbm.util.StringUtils;
import org.beetl.core.GroupTemplate;

public class GenerateControllerCode extends BaseGenerateCodeImpl {

    public GenerateControllerCode(GroupTemplate groupTemplate) {
        super(groupTemplate);
    }

    public void getSuperClass(GenerateSource generateSource) throws ClassNotFoundException {
        Class superclass = generateSource.getSuperclass();
        String extClass = null;
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
        if (StrUtil.isBlank(extClass)) {
            throw new ClassNotFoundException("未发现匹配的父类" + superclass.getName());
        }
        generateSource.getTemplate().binding("extClass", extClass);
        generateSource.getTemplate().binding("extClassName", StringUtils.substringAfterLast(extClass, "."));
    }


    @Override
    public CodeType getCodeType() {
        return CodeType.service;
    }
}
