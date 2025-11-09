package com.jbm.cluster.push.bevent.lis;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author wesley
 */
@Getter
public class ServiceOfflineEvent extends ApplicationEvent {
    private final String serviceId;

    public ServiceOfflineEvent(Object source, String serviceId) {
        super(source);
        this.serviceId = serviceId;
    }

}