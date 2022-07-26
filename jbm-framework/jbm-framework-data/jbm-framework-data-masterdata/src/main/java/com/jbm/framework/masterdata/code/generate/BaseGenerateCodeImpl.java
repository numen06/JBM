package com.jbm.framework.masterdata.code.generate;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import lombok.extern.slf4j.Slf4j;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.jbm.framework.masterdata.code.constants.CodeType.service;

@Slf4j
public abstract class BaseGenerateCodeImpl implements IGenerateCode {

    private final GroupTemplate groupTemplate;

    public BaseGenerateCodeImpl(GroupTemplate groupTemplate) {
        this.groupTemplate = groupTemplate;
    }

    @Override
    public File generate(GenerateSource generateSource) {
        if (this.isSkip(generateSource)) {
            return null;
        }
        return this.getWriteFile(generateSource);
    }


    @Override
    public Template createTemplate() {
        Template t = groupTemplate.getTemplate(this.getCodeType().name() + ".btl");
        return t;
    }

    @Override
    public File getWriteFile(GenerateSource generateSource) {
        CodeType codeType = this.getCodeType();
        URL url = ClassUtil.getResourceUrl("/", generateSource.getEntityClass());
        try {
            String temp = generateSource.getTargetPackage().replace(".", "/");
            Path wp = Paths.get(url.toURI()).getParent().getParent().resolve("src").resolve("main").resolve("java").resolve(temp);
            String ext = ".java";
            String suffix = StrUtil.upperFirst(codeType.name());
            switch (codeType) {
                case mapperXml:
                    wp = Paths.get(url.toURI()).getParent().getParent().resolve("src").resolve("main").resolve("resources").resolve("mapper");
                    ext = ".xml";
                    suffix = "Mapper";
                    break;
                case serviceImpl:
                    wp = wp.resolve(service.name()).resolve("impl");
                    break;
                default:
                    wp = wp.resolve(codeType.name());
                    break;
            }
            FileUtil.mkdir(wp.toFile());
            String fileName = generateSource.getEntityClass().getSimpleName() + suffix + ext;
            File file = wp.resolve(fileName).toFile();
            return file;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isSkip(GenerateSource generateSource) {
        if (generateSource.getSuperclass() == null) {
            log.debug("未检测到超类跳过:{}", generateSource.getEntityClass());
            return true;
        }
        if (ClassUtil.isAbstract(generateSource.getEntityClass())) {
            log.debug("该类为虚拟类:{}", generateSource.getEntityClass());
            return true;
        }
        if (ObjectUtil.isNotEmpty(generateSource.getIgnoreGeneate())) {
            if (ArrayUtil.contains(generateSource.getIgnoreGeneate().value(), this.getCodeType())) {
                log.debug("设置为忽略生成:{}", generateSource.getEntityClass());
                return true;
            }
        }
        if (ObjectUtil.isNotEmpty(generateSource)) {
            if (FileUtil.exist(generateSource.getTargetFile())) {
                return true;
            }
        }
        if (ObjectUtil.isEmpty(generateSource.getApiModel())) {
            throw new RuntimeException("该实体没有添加注解[ApiModel]");
        }


        return false;
    }
}
