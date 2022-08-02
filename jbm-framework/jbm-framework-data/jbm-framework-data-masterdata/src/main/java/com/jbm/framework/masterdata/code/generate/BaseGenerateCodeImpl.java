package com.jbm.framework.masterdata.code.generate;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.code.model.GenerateSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
public abstract class BaseGenerateCodeImpl implements IGenerateCode {


    public BaseGenerateCodeImpl() {
    }

    @Override
    public File generate(GenerateSource generateSource) {
//        if (this.pre(generateSource)) {
//            return null;
//        }
        this.getSuperClass(generateSource);
        return this.getWriteFile(generateSource);
    }


    @Override
    public String getTemplateName(GenerateSource generateSource) {
        return this.getCodeType().name();
    }

    @Override
    public Map<String, Object> getData() {
        return null;
    }

    @Override
    public File getWriteFile(GenerateSource generateSource) {
        File file = this.getTargetDir(generateSource).resolve(this.getCodeFileName(generateSource)).toFile();
        return file;
    }


    @SneakyThrows
    @Override
    public Path getTargetDir(GenerateSource generateSource) {
        URL url = ClassUtil.getResourceUrl("/", generateSource.getEntityClass());
        String temp = generateSource.getTargetPackage().replace(".", "/");
        String codeInPackge = StrUtil.replace(StrUtil.toUnderlineCase(this.getCodeType().name()), "_", "/");
        String codePackage = StrUtil.replace(StrUtil.concat(true, temp, "/", codeInPackge), "/", ".");
        generateSource.getData().put("codePackage", codePackage);
        Path wp = Paths.get(url.toURI()).getParent().getParent().resolve("src").resolve("main").resolve("java").resolve(temp).resolve(codeInPackge);
        PathUtil.mkdir(wp);
        return wp;
    }

    @Override
    public String getCodeFileName(GenerateSource generateSource) {
        CodeType codeType = this.getCodeType();
        String ext = ".java";
        String suffix = StrUtil.upperFirst(codeType.name());
        String fileName = generateSource.getEntityClass().getSimpleName() + suffix + ext;
        return fileName;
    }

    @Override
    public void pre(GenerateSource generateSource) {
        if (generateSource.getSuperclass() == null) {
            throw new ValidateException("未检测到超类跳过");
        }
        if (ClassUtil.isAbstract(generateSource.getEntityClass())) {
            throw new ValidateException("该类为虚拟类");
        } else {
            generateSource.getData().put("basePackage", generateSource.getTargetPackage());
            generateSource.getData().put("entityClass", generateSource.getEntityClass());
        }
        if (ObjectUtil.isNotEmpty(generateSource.getIgnoreGeneate())) {
            if (ArrayUtil.contains(generateSource.getIgnoreGeneate().value(), this.getCodeType())) {
                throw new ValidateException("设置为忽略生成");
            }
        }
        generateSource.setTargetDir(this.getTargetDir(generateSource));
        generateSource.setTargetFile(this.getWriteFile(generateSource));
        if (FileUtil.exist(generateSource.getTargetFile())) {
            throw new ValidateException(StrUtil.format("文件已经存在:{}", generateSource.getTargetFile()));
        } else {
            generateSource.getData().put("fileName", FileUtil.mainName(generateSource.getTargetFile()));
        }
        if (ObjectUtil.isEmpty(generateSource.getApiModel())) {
            throw new RuntimeException("该实体没有Swagger注解[ApiModel]");
        } else {
            generateSource.getData().put("entityDesc", generateSource.getApiModel().value());
        }
        generateSource.getData().put("entityFieldName", StrUtil.lowerFirst(ClassUtil.getClassName(generateSource.getEntityClass(), true)));
        generateSource.getData().put("time", DateUtil.now());
    }


}
