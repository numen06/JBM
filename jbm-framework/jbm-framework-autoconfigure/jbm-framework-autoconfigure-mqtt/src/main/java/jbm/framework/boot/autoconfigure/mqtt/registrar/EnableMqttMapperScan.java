package jbm.framework.boot.autoconfigure.mqtt.registrar;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({MqttScannerRegistrar.class})
public @interface EnableMqttMapperScan {
    String[] value() default {};

    String[] basePackages() default {};
}
