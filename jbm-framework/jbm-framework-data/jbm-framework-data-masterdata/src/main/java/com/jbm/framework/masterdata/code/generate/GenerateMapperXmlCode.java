package com.jbm.framework.masterdata.code.generate;

import cn.hutool.core.util.ClassUtil;
import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import com.jbm.util.StringUtils;
import lombok.SneakyThrows;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GenerateMapperXmlCode extends BaseGenerateCodeImpl {

    @Override
    public Path getModuleRootPath(URL url, GenerateSource generateSource) throws URISyntaxException {
        if (generateSource.getDaoModule() != null) {
            return Paths.get(url.toURI()).getParent().getParent().getParent().resolve(generateSource.getDaoModule());
        }
        return Paths.get(url.toURI()).getParent().getParent();
    }

    public String getSuperClass(GenerateSource generateSource) {
        String extClass = SuperMapper.class.getName();
        generateSource.getData().put("extClass", extClass);
        generateSource.getData().put("extClassName", StringUtils.substringAfterLast(extClass, "."));
        return extClass;
    }

    @SneakyThrows
    @Override
    public Path getTargetDir(GenerateSource generateSource) {
        URL url = ClassUtil.getResourceUrl("/", generateSource.getEntityClass());
        return this.getModuleRootPath(url, generateSource).resolve("src").resolve("main").resolve("resources").resolve("mapper");
    }

    @Override
    public String getCodeFileName(GenerateSource generateSource) {
        CodeType codeType = this.getCodeType();
        String fileName = generateSource.getEntityClass().getSimpleName() + "Mapper.xml";
        return fileName;
    }


    @Override
    public CodeType getCodeType() {
        return CodeType.mapperXml;
    }
}
