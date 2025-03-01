package jbm.framework.boot.autoconfigure.mqtt.annotation;

import java.lang.annotation.*;

/**
 * @author wesley
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqttCallEvent {
    String value() ;
}
