package com.jbm.framework.opcua.attribute;

import com.jbm.framework.opcua.event.ValueChanageEvent;
import lombok.Data;

/**
 * 监控的点位
 */
@Data
public class SubscriptionPoint {

    private OpcPoint point;
    private ValueChanageEvent callBackEvent;
}
