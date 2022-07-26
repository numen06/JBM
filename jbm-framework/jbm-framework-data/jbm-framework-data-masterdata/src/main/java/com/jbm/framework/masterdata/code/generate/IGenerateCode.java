package com.jbm.framework.masterdata.code.generate;

import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import org.beetl.core.Template;

import java.io.File;

public interface IGenerateCode {

    File generate(GenerateSource generateSource) throws Exception;


    boolean isSkip(GenerateSource generateSource);

    File getWriteFile(GenerateSource generateSource);


    CodeType getCodeType();

    Template createTemplate();

}
