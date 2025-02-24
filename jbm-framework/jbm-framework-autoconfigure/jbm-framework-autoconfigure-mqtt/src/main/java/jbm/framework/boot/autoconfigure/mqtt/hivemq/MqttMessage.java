package jbm.framework.boot.autoconfigure.mqtt.hivemq;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wesley
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttMessage {
    private String topic;
    private byte[] payload;
    private int qos = 0;

    public MqttMessage(String topic, byte[] payload) {
        this.topic = topic;
        this.payload = payload;
    }

    public MqttMessage(byte[] bytes) {
        this.payload = bytes;
    }

    public MqttMessage of(String topic, String payload) {
        if (StrUtil.isBlank(topic)) {
            throw new IllegalArgumentException("payload is blank");
        }
        return new MqttMessage(topic, payload.getBytes());
    }

    public MqttMessage of(String topic) {
        if (StrUtil.isBlank(topic)) {
            throw new IllegalArgumentException("payload is blank");
        }
        return new MqttMessage(this.topic, payload);
    }

    public MqttMessage json(String topic, Object payload) {
        if (StrUtil.isBlank(topic)) {
            throw new IllegalArgumentException("payload is blank");
        }
        return new MqttMessage(this.topic, JSON.toJSONString(payload).getBytes());
    }

}
