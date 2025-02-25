package jbm.framework.boot.autoconfigure.mqtt.registrar;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({MqttScannerRegistrar.class})
public @interface EnableMqttMapperScan {
    String[] value() default {};

    Class<?>[] basePackageClasses() default {};

    String[] basePackages() default {};
}
