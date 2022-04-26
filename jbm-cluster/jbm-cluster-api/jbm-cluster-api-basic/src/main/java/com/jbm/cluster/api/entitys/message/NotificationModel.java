package com.jbm.cluster.api.entitys.message;

import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class NotificationModel extends Notification {

    private String msgId;

    private Date createTime;

    private Date reviceTime;
    /**
     * 发送者
     */
    private String sender;
    /**
     * 接收者
     */
    private String recipient;

    public void sendBuild(String sender) {
        this.msgId = UUID.randomUUID().toString();
        this.sender = sender;
        this.createTime = new Date();
    }

    public void reviceBuild(String recipient) {
        this.reviceTime = new Date();
        this.recipient = recipient;
    }


}
