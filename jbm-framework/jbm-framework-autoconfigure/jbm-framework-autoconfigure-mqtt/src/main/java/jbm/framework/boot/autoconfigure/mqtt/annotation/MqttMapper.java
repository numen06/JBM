package jbm.framework.boot.autoconfigure.mqtt.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqttMapper {

    String clientId() default "";

    String value() default "";
}
