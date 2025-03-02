package jbm.framework.boot.autoconfigure.mqtt.useage;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.IdUtil;
import lombok.Data;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author wesley
 */
@Data
public class MqttCallContext {
    private final String eventId;
    private final String eventCode;
    private MqttCallBean requestBean;
    private MqttCallBean responseBean;
    private String requestTopic;
    private String responseTopic;
//    private Date requestTime;
//    private Date responseTime;
//    private Object requestMessage;
//    private Object responseBody;

    private final CountDownLatch latch = new CountDownLatch(1);


//    public MqttCallContext() {
//        this(IdUtil.simpleUUID());
//    }

    public MqttCallContext(String eventId, String eventCode) {
        this.eventId = eventId;
        this.eventCode = eventCode;
    }

    public MqttCallContext(String requestTopic, String responseTopic, String eventCode) {
        this(IdUtil.simpleUUID(), requestTopic, responseTopic, eventCode);
    }

    public MqttCallContext(String eventId, String requestTopic, String responseTopic, String eventCode) {
        this.eventId = eventId;
        this.requestTopic = requestTopic;
        this.responseTopic = responseTopic;
        this.eventCode = eventCode;
    }

    public MqttCallBean getIfRequestBean() {
        if (this.requestBean == null) {
            this.requestBean = new MqttCallBean(this.requestTopic, this.eventCode);
        }
        return this.requestBean;
    }

    public MqttCallBean getIfResponseBean() {
        if (this.responseBean == null) {
            this.responseBean = new MqttCallBean(this.responseTopic, this.eventCode);
        }
        return this.responseBean;
    }

    public  void fromRequestBean(String requestTopic, MqttCallBean requestBean) {
//        MqttCallContext context = new MqttCallContext(requestBean.getEventId(),requestTopic, null, requestBean.getEventCode());
        this.requestTopic = requestTopic;
        this.setRequestBean(requestBean);
    }

    public void putRequestMessage(Object requestMessage) {
        if (this.requestBean == null) {
            this.requestBean = new MqttCallBean();
        }
        if (this.requestBean.getEventId() == null) {
            this.requestBean.setEventId(this.eventId);
        }
        if (this.requestBean.getEventCode() == null) {
            this.requestBean.setEventCode(this.eventCode);
        }
        this.requestBean.setTime(DateTime.now());
        this.requestBean.setMessage(requestMessage);
    }

    public void putResponseBody(Object responseBody) {
        if (this.responseBean == null) {
            this.responseBean = new MqttCallBean();
        }
        if (this.responseBean.getEventId() == null) {
            this.responseBean.setEventId(this.eventId);
        }
        if (this.responseBean.getEventCode() == null) {
            this.responseBean.setEventCode(this.eventCode);
        }
        this.responseBean.setTime(DateTime.now());
        this.responseBean.setMessage(responseBody);

    }

    public void receiveResponse(MqttCallBean mqttCallBean) {
        this.responseBean = mqttCallBean;
        this.latch.countDown();
    }

}
