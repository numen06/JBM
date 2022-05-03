package com.jbm.cluster.common.basic;

import com.jbm.cluster.common.basic.bus.event.RemoteRefreshRouteEvent;
import jbm.framework.boot.autoconfigure.eventbus.publisher.ClusterEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 自定义RestTemplate请求工具类
 *
 * @author: wesley.zhang
 * @date: 2018/12/11 15:51
 * @description:
 */
@Slf4j
public class JbmClusterTemplate {


    @Autowired
    private ClusterEventPublisher clusterEventPublisher;

    public JbmClusterTemplate() {
    }


    /**
     * 刷新网关
     * 注:不要频繁调用!
     * 1.资源权限发生变化时可以调用
     * 2.流量限制变化时可以调用
     * 3.IP访问发生变化时可以调用
     * 4.智能路由发生变化时可以调用
     */
    public void refreshGateway() {
        try {
            clusterEventPublisher.publishEvent(new RemoteRefreshRouteEvent());
            log.info("发送刷新网关事件");
        } catch (Exception e) {
            log.info("发送刷新网关事件失败", e);
        }
    }

}
