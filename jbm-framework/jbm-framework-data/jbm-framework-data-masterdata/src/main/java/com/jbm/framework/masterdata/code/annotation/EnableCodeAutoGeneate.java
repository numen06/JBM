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
     *
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
     *
     * @return
     */
    Class<?>[] entityPackageClasses() default {};

    /**
     * 代码生成目录
     *
     * @return
     */
    String targetPackage();

    /**
     * Mapper 模块配置（dao/mapper/mapperXml 的 module 与 packageBase），未配置时由平铺属性或 targetPackage 派生。
     */
    MapperConfig mapper() default @MapperConfig();

    /**
     * Service 模块配置（service/serviceImpl 的 module 与 packageBase）。
     */
    ServiceConfig service() default @ServiceConfig();

    /**
     * Controller 模块配置（controller 的 module 与 packageBase）。
     */
    ControllerConfig controller() default @ControllerConfig();

    /**
     * Business 模块配置（business/businessImpl 的 module 与 packageBase）。
     */
    BusinessConfig business() default @BusinessConfig();

    /**
     * 排除包目录
     */
    String[] excludePackages() default {};

    /**
     * Mapper 模块独立配置，仅作用于 dao/mapper/mapperXml。
     */
    @Retention(RetentionPolicy.RUNTIME)
    @interface MapperConfig {
        String module() default "";
        String packageBase() default "";
        /**
         * Mapper XML 输出目录名（相对 resources 下），如 test、mapper；为空时使用 mapper。
         */
        String mapperXmlDir() default "";
        /** 是否生成 Mapper 模块（mapper + mapperXml），默认 true */
        boolean enabled() default true;
    }

    /**
     * Service 模块独立配置，仅作用于 service/serviceImpl。
     */
    @Retention(RetentionPolicy.RUNTIME)
    @interface ServiceConfig {
        String module() default "";
        String packageBase() default "";
        /** 是否生成 Service 模块（service + serviceImpl），默认 true */
        boolean enabled() default true;
    }

    /**
     * Controller 模块独立配置。
     */
    @Retention(RetentionPolicy.RUNTIME)
    @interface ControllerConfig {
        String module() default "";
        String packageBase() default "";
        /** 是否生成 Controller 模块，默认 true */
        boolean enabled() default true;
    }

    /**
     * Business 模块独立配置，仅作用于 business/businessImpl。
     */
    @Retention(RetentionPolicy.RUNTIME)
    @interface BusinessConfig {
        String module() default "";
        String packageBase() default "";
        /** 是否生成 Business 模块（business + businessImpl），默认 true */
        boolean enabled() default true;
    }
}
