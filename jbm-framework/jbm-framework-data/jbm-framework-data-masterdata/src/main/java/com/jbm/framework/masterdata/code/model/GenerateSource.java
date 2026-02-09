package com.jbm.framework.masterdata.code.model;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.template.Template;
import com.jbm.framework.masterdata.code.annotation.BussinessGroup;
import com.jbm.framework.masterdata.code.annotation.IgnoreGeneate;
import com.jbm.framework.masterdata.code.constants.CodeType;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * 生产源
 */
@Data
public class GenerateSource {

    /**
     * 业务分组
     */
//    private List<Class> bussinessGroupList = new ArrayList<>();
    private Set<Class> bussinessEntityList = new HashSet<>();

    /**
     * 实体类
     */
    private Class<?> entityClass;
    /**
     * 超类
     */
    private Class superclass;

    /**
     * 忽略层
     */
    private List<String> ignoreCodeType;

    /**
     * 代码生成模板
     */
    private Template template;
    /**
     * 生产的包名
     */
    private String targetPackage;

    /**
     * 模块名
     */
    private String serviceModule;

    /**
     * dao模块名
     */
    private String daoModule;

    /**
     * Mapper 接口输出模块路径
     */
    private String mapperModule;

    /**
     * Mapper XML 输出模块路径
     */
    private String mapperXmlModule;

    /**
     * ServiceImpl 输出模块路径
     */
    private String serviceImplModule;

    /**
     * Controller 输出模块路径
     */
    private String controllerModule;

    /**
     * Business 接口输出模块路径
     */
    private String businessModule;

    /**
     * BusinessImpl 输出模块路径
     */
    private String businessImplModule;

    /**
     * 具体包的位置
     */
    private String codePackage;
    /**
     * 目标文件文件夹
     */
    private Path targetDir;
    /**
     * 生成代码时候的数据
     */
    private Map<String, Object> data = new HashMap<>();


    private File targetFile;

    private IgnoreGeneate ignoreGeneate;


    private BussinessGroup bussinessGroup;

    private ApiModel apiModel;

    /**
     * 按代码类型解析输出模块路径。未配置时返回 null 表示使用当前应用模块。
     */
    public String getOutputModuleFor(CodeType codeType) {
        if (codeType == null) {
            return null;
        }
        switch (codeType) {
            case mapper:
                return StrUtil.emptyToDefault(StrUtil.trimToNull(mapperModule), daoModule);
            case mapperXml:
                return StrUtil.emptyToDefault(StrUtil.trimToNull(mapperXmlModule), daoModule);
            case service:
                return StrUtil.trimToNull(serviceModule);
            case serviceImpl:
                return StrUtil.emptyToDefault(StrUtil.trimToNull(serviceImplModule), serviceModule);
            case controller:
                return StrUtil.trimToNull(controllerModule);
            case business:
                return StrUtil.trimToNull(businessModule);
            case businessImpl:
                return StrUtil.trimToNull(businessImplModule);
            default:
                return null;
        }
    }

}
