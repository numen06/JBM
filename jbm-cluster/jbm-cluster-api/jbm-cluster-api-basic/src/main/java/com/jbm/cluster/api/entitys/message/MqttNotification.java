package com.jbm.cluster.api.entitys.message;

import lombok.Data;

/**
 * @author wesley.zhang
 * @date 2018-3-27
 **/
@Data
public class MqttNotification extends NotificationModel {

    /**
     * 通道
     */
    private String topic;

    /**
     * 消息体
     */
    private Object body;

    /**
     * 送达设置
     */
    private Integer qos = 1;

}
