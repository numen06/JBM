package com.jbm.cluster.push.usage;

import com.jbm.cluster.api.model.message.Notification;


/**
 * 消息处理类
 */
public interface NotificationExchanger {

    boolean support(Object notification);

    boolean exchange(Notification notification);
}
