package com.jbm.cluster.api.model.message;

import lombok.Data;

import java.util.Map;

/**
 * @author wesley.zhang
 * @date 2018-3-27
 **/
@Data
public class MqttNotification implements Notification {

    /**
     * 用户id
     */
    private String userId;

    /**
     * 标签组：a,b,c
     */
    private String tags;

    /**
     * 标题头
     */
    private String title;
    /**
     * 消息体
     */
    private String body;
    /**
     * 参数
     */
    private Map<String, Object> params;
    /**
     * 类型
     */
    private String type;
    /**
     * 等级
     */
    private Integer level;

    private String msgId;
}
