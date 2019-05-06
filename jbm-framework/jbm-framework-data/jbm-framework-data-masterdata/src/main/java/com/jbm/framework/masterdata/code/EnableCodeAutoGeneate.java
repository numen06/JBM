package com.jbm.framework.masterdata.code;

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
    @AliasFor("entityPackages")
    String[] value() default {};

    @AliasFor("value")
    String[] entityPackages() default {};

    Class<?>[] entityPackageClasses() default {};

    String targetPackage();
}
