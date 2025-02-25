package jbm.framework.boot.autoconfigure.mqtt.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqttSend {

    String toTopic();
}
