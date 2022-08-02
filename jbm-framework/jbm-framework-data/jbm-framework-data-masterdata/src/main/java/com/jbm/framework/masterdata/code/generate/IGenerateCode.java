package com.jbm.framework.masterdata.code.generate;

import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public interface IGenerateCode {

    File generate(GenerateSource generateSource) throws Exception;


    void pre(GenerateSource generateSource);

    File getWriteFile(GenerateSource generateSource);


    Path getTargetDir(GenerateSource generateSource);

    String getCodeFileName(GenerateSource generateSource);

    CodeType getCodeType();

    String getTemplateName(GenerateSource generateSource);

    Map<String,Object> getData();

    String getSuperClass(GenerateSource generateSource);

}
