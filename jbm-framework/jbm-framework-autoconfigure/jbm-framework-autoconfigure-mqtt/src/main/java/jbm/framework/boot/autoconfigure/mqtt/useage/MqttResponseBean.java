package jbm.framework.boot.autoconfigure.mqtt.useage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttResponseBean {

    private String topic;

    private Object body;

    private int qos = 0;

    public MqttResponseBean(String topic, Object body) {
        this.topic = topic;
        this.body = body;
    }

}
