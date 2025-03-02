package jbm.framework.boot.autoconfigure.mqtt.useage;

import lombok.Data;

/**
 * @author wesley
 */
@Data
public class MqttCallEventBean {

    private String topic;
    private String eventId;
    private String time;
    private String eventCode;
    private Object message;

}
