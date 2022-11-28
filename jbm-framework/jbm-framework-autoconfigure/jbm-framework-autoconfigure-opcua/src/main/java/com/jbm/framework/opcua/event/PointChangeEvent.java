package com.jbm.framework.opcua.event;

import com.jbm.framework.opcua.attribute.OpcPoint;
import com.jbm.framework.opcua.util.ReflectUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

/**
 * @Author fanscat
 * @CreateTime 2022/10/31 18:43
 * @Description
 */
@Slf4j
@Getter
public class PointChangeEvent extends PointSubscribeEvent {

    private String device;
    private Object target;

    public PointChangeEvent(Object source, String device, String column, Object obj) {
        super(source, new OpcPoint(column));
        this.device = device;
        this.target = obj;
    }

    @Override
    public void putData(UaMonitoredItem uaMonitoredItem, DataValue dataValue) {
        super.putData(uaMonitoredItem, dataValue);
        String column = ReflectUtils.underlineToCamel(getOpcPoint().getAlias());
        log.info("设备[{}]点位[{}]数据发生变化[{}]==>[{}]", getDevice(), getOpcPoint().getAlias(), ReflectUtils.getFieldValue(getTarget(), column), getOpcPoint().getValue());
        ReflectUtils.setFieldValue(getTarget(), column, getOpcPoint().getValue());
    }
}
