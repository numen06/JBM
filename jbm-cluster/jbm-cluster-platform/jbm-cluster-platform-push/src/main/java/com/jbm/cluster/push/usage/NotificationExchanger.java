package com.jbm.cluster.push.usage;

import com.jbm.cluster.api.entitys.message.Notification;


/**
 * 消息处理类
 */
public interface NotificationExchanger<T extends Notification> {

    boolean support(Object notification);

    boolean exchange(Notification notification);

    boolean process(T notification);

}
