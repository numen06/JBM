package jbm.framework.boot.autoconfigure.mqtt.event;

import org.springframework.context.ApplicationEvent;

/**
 * MQTT 客户端连接成功事件，用于在连接建立后立即恢复订阅，避免 "No publish flow registered" 警告。
 *
 * @author wesley
 */
public class MqttConnectedEvent extends ApplicationEvent {

    public MqttConnectedEvent(Object source) {
        super(source);
    }
}
