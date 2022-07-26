package com.jbm.framework.masterdata.code.generate;

import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.service.IMultiPlatformService;
import com.jbm.framework.masterdata.usage.entity.*;
import com.jbm.util.StringUtils;
import org.beetl.core.GroupTemplate;

public class GenerateBusinessCode extends BaseGenerateCodeImpl {

    public GenerateBusinessCode(GroupTemplate groupTemplate) {
        super(groupTemplate);
    }

    public void getSuperClass(GenerateSource generateSource) {
        Class superclass = generateSource.getSuperclass();
        String extClass = null;
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
        generateSource.getTemplate().binding("extClass", extClass);
        generateSource.getTemplate().binding("extClassName", StringUtils.substringAfterLast(extClass, "."));
    }


    @Override
    public CodeType getCodeType() {
        return CodeType.service;
    }
}
