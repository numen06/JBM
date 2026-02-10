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
     * Mapper 层 Java 包名（未配置时回退 targetPackage + ".mapper"）
     */
    private String mapperPackage;

    /**
     * Mapper XML 输出目录名（相对 resources 下），如 test、mapper；未配置时使用 mapper。
     */
    private String mapperXmlDir;

    /**
     * Service 层 Java 包名（未配置时回退 targetPackage + ".service"）
     */
    private String servicePackage;

    /**
     * Controller 层 Java 包名（未配置时回退 targetPackage + ".controller"）
     */
    private String controllerPackage;

    /**
     * Business 层 Java 包名（未配置时回退 targetPackage + ".business"）
     */
    private String businessPackage;

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

    /**
     * 按代码类型解析 Java 包名。未配置 *Package 时回退为 targetPackage + 类型对应路径段（与 BaseGenerateCodeImpl 路径一致）。
     * mapperXml 的 namespace 使用 mapperPackage；serviceImpl 在 servicePackage 下加 impl 子包；businessImpl 回退到 businessPackage。
     */
    public String getPackageFor(CodeType codeType) {
        if (codeType == null || StrUtil.isBlank(targetPackage)) {
            return targetPackage;
        }
        String configured = null;
        boolean appendImpl = false;
        switch (codeType) {
            case mapper:
                configured = StrUtil.trimToNull(mapperPackage);
                break;
            case mapperXml:
                configured = StrUtil.trimToNull(mapperPackage);
                break;
            case service:
                configured = StrUtil.trimToNull(servicePackage);
                break;
            case serviceImpl:
                configured = StrUtil.trimToNull(servicePackage);
                appendImpl = true;
                break;
            case controller:
                configured = StrUtil.trimToNull(controllerPackage);
                break;
            case business:
                configured = StrUtil.trimToNull(businessPackage);
                break;
            case businessImpl:
                configured = StrUtil.trimToNull(businessPackage);
                break;
            default:
                break;
        }
        if (StrUtil.isNotBlank(configured)) {
            return appendImpl ? configured + ".impl" : configured;
        }
        String segment = getPackageSegmentFor(codeType);
        return StrUtil.isBlank(segment) ? targetPackage : targetPackage + "." + segment;
    }

    /**
     * CodeType 对应的包路径段（点号分隔），与 BaseGenerateCodeImpl 中 codeInPackge 的目录结构对应。
     */
    private static String getPackageSegmentFor(CodeType codeType) {
        if (codeType == null) {
            return "";
        }
        switch (codeType) {
            case mapper:
                return "mapper";
            case mapperXml:
                return "mapper.xml";
            case service:
                return "service";
            case serviceImpl:
                return "service.impl";
            case controller:
                return "controller";
            case business:
                return "business";
            case businessImpl:
                return "business.impl";
            default:
                return "";
        }
    }

}
