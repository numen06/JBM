package com.jbm.cluster.common.basic;

import cn.hutool.core.date.DateTime;
import com.jbm.cluster.api.bus.event.RemoteRefreshRouteEvent;
import com.jbm.util.batch.BatchTask;
import jbm.framework.boot.autoconfigure.eventbus.publisher.ClusterEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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


    private BatchTask<Date> batchTask = new BatchTask<>(30L, TimeUnit.SECONDS, 200, new Consumer<List<Date>>() {
        @Override
        public void accept(List<Date> gatewayLogs) {
            try {
                clusterEventPublisher.publishEvent(new RemoteRefreshRouteEvent());
                log.info("发送刷新网关事件");
            } catch (Exception e) {
                log.info("发送刷新网关事件失败", e);
            }
        }
    });

    /**
     * 刷新网关
     * 注:不要频繁调用!
     * 1.资源权限发生变化时可以调用
     * 2.流量限制变化时可以调用
     * 3.IP访问发生变化时可以调用
     * 4.智能路由发生变化时可以调用
     */
    public void refreshGateway() {
        batchTask.offer(DateTime.now());
    }

}
