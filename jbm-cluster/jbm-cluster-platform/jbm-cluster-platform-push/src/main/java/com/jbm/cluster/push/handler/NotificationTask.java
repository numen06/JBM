package com.jbm.cluster.push.handler;

import com.jbm.cluster.api.model.entitys.message.Notification;
import com.jbm.cluster.push.usage.NotificationExchanger;

import java.util.concurrent.Callable;

/**
 * @author wesley.zhang
 * @date 2018-3-31
 **/
public class NotificationTask implements Callable<Boolean> {

    private NotificationExchanger notificationExchanger;

    private Notification notification;

    public NotificationTask(NotificationExchanger notificationExchanger, Notification notification) {
        this.notificationExchanger = notificationExchanger;
        this.notification = notification;
    }

    @Override
    public Boolean call() throws Exception {
        return notificationExchanger.exchange(notification);
    }
}
