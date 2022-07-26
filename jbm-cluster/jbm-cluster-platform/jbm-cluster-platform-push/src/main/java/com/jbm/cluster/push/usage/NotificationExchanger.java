package com.jbm.cluster.push.usage;

import com.jbm.cluster.api.entitys.message.Notification;
import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.api.model.push.PushCallback;

import java.util.function.Function;


/**
 * 消息处理类
 */
public interface NotificationExchanger<T extends Notification> extends Function<T, PushCallback> {

    boolean support(Object notification);

//    boolean exchange(T notification);

//    boolean process(T notification);

    T build(PushMessageBody pushMessageBody, PushMessageItem pushMessageItem);

    void send(T notification);
}
