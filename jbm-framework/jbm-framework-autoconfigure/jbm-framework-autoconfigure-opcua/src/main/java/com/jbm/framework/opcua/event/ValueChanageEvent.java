package com.jbm.framework.opcua.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.springframework.context.ApplicationEvent;

@EqualsAndHashCode(callSuper = true)
@Data
public class ValueChanageEvent extends ApplicationEvent {

    private DataValue dataValue;

    /**
     * Create a new ApplicationEvent.
     *
     * @param uaMonitoredItem the object on which the event initially occurred (never {@code null})
     */
    public ValueChanageEvent(UaMonitoredItem uaMonitoredItem) {
        super(uaMonitoredItem);
    }

    public ValueChanageEvent(UaMonitoredItem uaMonitoredItem, DataValue dataValue) {
        super(uaMonitoredItem);
        this.dataValue = dataValue;
    }

    public UaMonitoredItem getUaMonitoredItem() {
        return (UaMonitoredItem) this.source;
    }

}
