package com.jbm.framework.opcua.event;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.IdUtil;
import com.jbm.framework.opcua.attribute.OpcPoint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.springframework.context.ApplicationEvent;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class ValueChanageEvent extends ApplicationEvent {

    private DataValue dataValue;

    private String eventId = IdUtil.fastUUID();

    private Date sendTime;

    private UaMonitoredItem uaMonitoredItem;

    public ValueChanageEvent(Object source) {
        super(source);
    }

    public void putData(UaMonitoredItem uaMonitoredItem, DataValue dataValue) {
        this.uaMonitoredItem = uaMonitoredItem;
        this.dataValue = dataValue;
        sendTime = DateTime.now();
    }

}
