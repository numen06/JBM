package jbm.framework.boot.autoconfigure.mqtt.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.SpringApplicationEvent;

/**
 * @author wesley
 */
public class MqttMapperSubscribeEvent extends ApplicationEvent  {
    public MqttMapperSubscribeEvent( String[] args) {
        super(application, args);
    }
}
