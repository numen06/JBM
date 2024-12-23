package com.jbm.framework.masterdata.code.generate;

import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import com.jbm.util.StringUtils;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author wesley
 */
public class GenerateMapperCode extends BaseGenerateCodeImpl {


    @Override
    public Path getModuleRootPath(URL url, GenerateSource generateSource) throws URISyntaxException {
        if (generateSource.getDaoModule() != null) {
            return Paths.get(url.toURI()).getParent().getParent().getParent().resolve(generateSource.getDaoModule());
        }
        return Paths.get(url.toURI()).getParent().getParent();
    }

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
