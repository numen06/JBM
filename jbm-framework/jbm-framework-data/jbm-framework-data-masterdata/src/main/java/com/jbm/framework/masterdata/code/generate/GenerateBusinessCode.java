package com.jbm.framework.masterdata.code.generate;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.masterdata.business.IPlatformBusiness;
import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;

import java.io.File;

public class GenerateBusinessCode extends BaseGenerateCodeImpl {


    @Override
    public File getWriteFile(GenerateSource generateSource) {
        CodeType codeType = this.getCodeType();
        String ext = ".java";
        String suffix = StrUtil.upperFirst(codeType.name());
        String fileName = GenerateBusinessImplCode.getBusinessName(generateSource) + suffix + ext;
        File file = this.getTargetDir(generateSource).resolve(fileName).toFile();
        return file;
    }

    public String getSuperClass(GenerateSource generateSource) {
        Class extClass = IPlatformBusiness.class;
        generateSource.getData().put("extClass", extClass.getName());
        generateSource.getData().put("extClassName", ClassUtil.getClassName(extClass, true));
        return extClass.getName();
    }

    @Override
    public File generate(GenerateSource generateSource) {
        return super.generate(generateSource);
    }

    @Override
    public CodeType getCodeType() {
        return CodeType.business;
    }

    @Override
    public void pre(GenerateSource generateSource) {
        super.pre(generateSource);
        if (generateSource.getBussinessGroup() == null) {
            throw new ValidateException("不需要业务分类");
        }
    }
}
