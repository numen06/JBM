package com.jbm.cluster.logs.event;

import com.jbm.cluster.logs.entity.GatewayLogs;
import lombok.Data;
import org.springframework.context.ApplicationEvent;


@Data
public class AccessEvent extends ApplicationEvent {

    private final GatewayLogs gatewayLogs;

    public AccessEvent(Object source, GatewayLogs gatewayLogs) {
        super(source);
        this.gatewayLogs = gatewayLogs;
    }
}
