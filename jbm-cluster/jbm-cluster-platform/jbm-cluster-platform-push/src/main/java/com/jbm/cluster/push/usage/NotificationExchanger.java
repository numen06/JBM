package com.jbm.cluster.push.usage;

import com.jbm.cluster.api.model.entitys.message.Notification;


/**
 * 消息处理类
 */
public interface NotificationExchanger {

    boolean support(Object notification);

    boolean exchange(Notification notification);
}
