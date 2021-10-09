package com.jbm.cluster.api.model.entitys.message;

import lombok.Data;

import java.util.Date;

@Data
public class NotificationModel implements Notification {

    private String msgId;

    private Date createTime;

}
