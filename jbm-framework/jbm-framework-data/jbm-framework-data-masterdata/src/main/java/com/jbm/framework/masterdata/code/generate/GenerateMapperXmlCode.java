package com.jbm.framework.masterdata.code.generate;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import com.jbm.util.StringUtils;
import lombok.SneakyThrows;

import java.net.URL;
import java.nio.file.Path;

public class GenerateMapperXmlCode extends BaseGenerateCodeImpl {

    private static final String DEFAULT_MAPPER_XML_DIR = "mapper";

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
        String dirName = resolveMapperXmlDirName(generateSource);
        return this.getModuleRootPath(url, generateSource).resolve("src").resolve("main").resolve("resources").resolve(dirName);
    }

    /**
     * mapperXmlDir 已设置时用其值作为目录名（如 test、mapper），未设置时用默认 mapper。
     * 优先读属性，再从 data 回退（确保注解配置一定能传到生成目录）。
     */
    private static String resolveMapperXmlDirName(GenerateSource generateSource) {
        String dir = StrUtil.trimToEmpty(generateSource.getMapperXmlDir());
        if (StrUtil.isBlank(dir)) {
            Object fromData = generateSource.getData() != null ? generateSource.getData().get("mapperXmlDir") : null;
            dir = fromData != null ? StrUtil.trimToEmpty(fromData.toString()) : "";
        }
        return StrUtil.isBlank(dir) ? DEFAULT_MAPPER_XML_DIR : dir;
    }

    @Override
    public String getCodeFileName(GenerateSource generateSource) {
        return generateSource.getEntityClass().getSimpleName() + "Mapper.xml";
    }


    @Override
    public CodeType getCodeType() {
        return CodeType.mapperXml;
    }
}
