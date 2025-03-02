package jbm.framework.boot.autoconfigure.mqtt.useage;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.Date;


/**
 * @author wesley
 */
@Data
public class MqttCallBean {
    private String eventId;
    private String eventCode;
    private Date time;
//    private String topic;
    private Object message;

    public MqttCallBean() {
    }

    public MqttCallBean(String eventId, String eventCode) {
        this.eventId = eventId;
        this.eventCode = eventCode;
    }

}
