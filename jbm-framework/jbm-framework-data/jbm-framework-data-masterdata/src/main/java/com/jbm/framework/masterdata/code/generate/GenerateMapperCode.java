package com.jbm.framework.masterdata.code.generate;

import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import com.jbm.util.StringUtils;
import org.beetl.core.GroupTemplate;

public class GenerateMapperCode extends BaseGenerateCodeImpl {

    public GenerateMapperCode(GroupTemplate groupTemplate) {
        super(groupTemplate);
    }


    public void getSuperClass(GenerateSource generateSource) {
        String extClass = SuperMapper.class.getName();
        generateSource.getTemplate().binding("extClass", extClass);
        generateSource.getTemplate().binding("extClassName", StringUtils.substringAfterLast(extClass, "."));
    }


    @Override
    public CodeType getCodeType() {
        return CodeType.service;
    }
}
