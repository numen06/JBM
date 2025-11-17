package com.jbm.cluster.push.bevent.lis;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 服务上线事件
 * @author wesley
 */
@Getter
public class ServiceOnlineEvent extends ApplicationEvent {
    private final String serviceId;

    public ServiceOnlineEvent(Object source, String serviceId) {
        super(source);
        this.serviceId = serviceId;
    }

}