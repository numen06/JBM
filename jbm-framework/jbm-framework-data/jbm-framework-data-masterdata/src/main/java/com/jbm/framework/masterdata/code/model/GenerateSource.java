package com.jbm.framework.masterdata.code.model;

import com.jbm.framework.masterdata.code.annotation.IgnoreGeneate;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.beetl.core.Template;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 生产源
 */
@Data
public class GenerateSource {

    /**
     * 业务分组
     */
    private List<String> bussinessGroup;

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
     * 生成代码时候的数据
     */
    private Map<String, Object> data;


    private File targetFile;

    private IgnoreGeneate ignoreGeneate;

    private ApiModel apiModel;


}
