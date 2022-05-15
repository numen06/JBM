package com.jbm.cluster.api.bus.event;

import jbm.framework.boot.autoconfigure.eventbus.model.AbstractClusterEvent;

/**
 * 自定义网关刷新远程事件
 *
 * @author wesley.zhang
 */
public class RemoteRefreshRouteEvent extends AbstractClusterEvent {

    public RemoteRefreshRouteEvent() {
    }

}
