
package com.jbm.cluster.push.handler;

import com.jbm.cluster.api.entitys.message.Notification;
import com.jbm.cluster.push.usage.NotificationExchanger;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

/**
 * @author wesley.zhang
 * @date 2018-3-31
 **/
@Slf4j
public class NotificationTask implements Callable<Boolean> {

    private NotificationExchanger notificationExchanger;

    private Notification notification;

    public NotificationTask(NotificationExchanger notificationExchanger, Notification notification) {
        this.notificationExchanger = notificationExchanger;
        this.notification = notification;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            notificationExchanger.exchange(notification);
        } catch (Exception e) {
            log.error("处理消息错误", e);
            return false;
        }
        return true;
    }
}
