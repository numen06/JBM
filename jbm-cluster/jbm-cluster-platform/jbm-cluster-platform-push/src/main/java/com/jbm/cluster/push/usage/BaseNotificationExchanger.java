package com.jbm.cluster.push.usage;

import cn.hutool.core.util.TypeUtil;
import com.jbm.cluster.api.entitys.message.Notification;

/**
 * @Created wesley.zhang
 * @Date 2022/4/25 13:19
 * @Description TODO
 */
public abstract class BaseNotificationExchanger<T extends Notification> implements NotificationExchanger<T> {

    private Class baseType;

    @Override
    public boolean support(Object notification) {
        if (notification == null) {
            return false;
        }
        if (baseType == null) {
            baseType = TypeUtil.getClass(TypeUtil.getTypeArgument(getClass(), 0));
        }
        return baseType.equals(notification.getClass());
    }

    @Override
    public boolean exchange(Notification notification) {
        return process((T) notification);
    }
}
