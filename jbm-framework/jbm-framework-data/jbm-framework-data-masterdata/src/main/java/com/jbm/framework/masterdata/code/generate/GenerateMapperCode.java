package com.jbm.framework.masterdata.code.generate;

import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import com.jbm.util.StringUtils;
import org.beetl.core.GroupTemplate;

public class GenerateMapperCode extends BaseGenerateCodeImpl {



    @Override
    public String getSuperClass(GenerateSource generateSource) {
        String extClass = SuperMapper.class.getName();
        generateSource.getData().put("extClass", extClass);
        generateSource.getData().put("extClassName", StringUtils.substringAfterLast(extClass, "."));
        return extClass;
    }


    @Override
    public CodeType getCodeType() {
        return CodeType.mapper;
    }
}
