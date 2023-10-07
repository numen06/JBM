package com.jbm.framework.masterdata.code.model;

import cn.hutool.extra.template.Template;
import com.jbm.framework.masterdata.code.annotation.BussinessGroup;
import com.jbm.framework.masterdata.code.annotation.IgnoreGeneate;
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


}
