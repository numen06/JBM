package com.jbm.framework.opcua.event;

import cn.hutool.core.util.ReflectUtil;
import com.jbm.framework.opcua.annotation.OpcUaHeartBeat;
import com.jbm.framework.opcua.attribute.OpcBean;
import com.jbm.framework.opcua.attribute.OpcPoint;
import com.jbm.framework.opcua.util.ReflectUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import java.lang.reflect.Field;

/**
 * @author fanscat
 * @createTime 2022/10/31 18:43
 */
@Slf4j
@Getter
public class PointChangeEvent<T extends OpcBean> extends PointSubscribeEvent {

    private String device;
    private T target;

    public PointChangeEvent(T source, String device, String alias) {
        super(alias, new OpcPoint(alias));
        this.device = device;
        this.target = source;
    }

    @Override
    public void putData(UaMonitoredItem uaMonitoredItem, DataValue dataValue) {
        super.putData(uaMonitoredItem, dataValue);
        final Field field = ReflectUtils.getReadField(this.getTarget(), super.getOpcPoint().getAlias());
        if (!field.isAnnotationPresent(OpcUaHeartBeat.class)) {
            // 心跳点位读写频率太高，输出日志时排除心跳
            log.info("设备[{}]点位[{}]数据发生变化[{}]==>[{}]", getDevice(), getSource(), ReflectUtil.getFieldValue(this.getTarget(), field), super.getOpcPoint().getValue());
        }
        ReflectUtils.setFieldValue(this.getTarget(), field, super.getOpcPoint().getValue());
    }
}
