package com.jbm.framework.opcua.event;

import com.jbm.framework.opcua.attribute.OpcPoint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

@Data
@EqualsAndHashCode(callSuper = true)
public class PointSubscribeEvent extends ValueChanageEvent {

    private final OpcPoint opcPoint;

    public PointSubscribeEvent(Object source, OpcPoint point) {
        super(source);
        this.opcPoint = point;
    }

    public void putData(UaMonitoredItem uaMonitoredItem, DataValue dataValue) {
        super.putData(uaMonitoredItem, dataValue);
        this.opcPoint.setValue(dataValue.getValue().getValue());
    }

}
