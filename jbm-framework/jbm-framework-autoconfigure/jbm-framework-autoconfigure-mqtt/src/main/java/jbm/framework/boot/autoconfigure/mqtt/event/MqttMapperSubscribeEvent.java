package jbm.framework.boot.autoconfigure.mqtt.event;

import org.springframework.context.ApplicationEvent;

/**
 * @author wesley
 */
public class MqttMapperSubscribeEvent extends ApplicationEvent {

    public MqttMapperSubscribeEvent(Object source) {
        super(source);
    }
}
