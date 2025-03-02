package jbm.framework.boot.autoconfigure.mqtt.annotation.call;

import java.lang.annotation.*;

/**
 * @author wesley
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqttCallClient {

    String clientId() default "";

    String requestTopic();

    String responseTopic();
}
