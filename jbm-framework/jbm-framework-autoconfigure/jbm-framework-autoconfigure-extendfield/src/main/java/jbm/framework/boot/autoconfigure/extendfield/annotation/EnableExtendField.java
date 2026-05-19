package jbm.framework.boot.autoconfigure.extendfield.annotation;

import jbm.framework.boot.autoconfigure.extendfield.ExtendFieldAutoConfiguration;
import jbm.framework.boot.autoconfigure.extendfield.FieldDefinitionSource;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用扩展字段（AOP + 字段定义服务）。
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ExtendFieldAutoConfiguration.class)
public @interface EnableExtendField {

    FieldDefinitionSource source() default FieldDefinitionSource.REDIS;
}
