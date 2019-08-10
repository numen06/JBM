package com.jbm.autoconfig.dic.annotation;

import com.jbm.autoconfig.dic.EnumScanPackages;
import jbm.framework.boot.autoconfigure.dictionary.DictionaryAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({EnumScanPackages.Registrar.class, DictionaryAutoConfiguration.class})
public @interface EnableJbmDictionary {

    @AliasFor("basePackages")
    String[] value() default {};

    @AliasFor("value")
    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};
}
