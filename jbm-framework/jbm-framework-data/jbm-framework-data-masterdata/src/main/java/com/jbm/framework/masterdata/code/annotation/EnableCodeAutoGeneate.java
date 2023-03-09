package com.jbm.framework.masterdata.code.annotation;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author: create by wesley
 * @date:2019/4/28
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({AutoScanCodePackages.CodeRegistrar.class})
public @interface EnableCodeAutoGeneate {
    /**
     * 需要生产实体的扫描包路径
     * @return
     */
    @AliasFor("entityPackages")
    String[] value() default {};
    /**
     * @entityPackages
     */
    @AliasFor("value")
    String[] entityPackages() default {};
    /**
     * 需要生产实体的扫描包的一个或多个实体
     * @return
     */
    Class<?>[] entityPackageClasses() default {};

    /**
     * 代码生成目录
     * @return
     */
    String targetPackage();
}
