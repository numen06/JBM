package jbm.framework.boot.autoconfigure.eventbus.publisher;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import jbm.framework.boot.autoconfigure.eventbus.model.AbstractClusterEvent;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.PathDestinationFactory;
import org.springframework.context.ApplicationEventPublisher;


/**
 * @author wesley.zhang
 * @description 集群事件发布者
 */
public class ClusterEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final BusProperties busProperties;

    private static ClusterEventPublisher clusterEventPublisher;

    private final PathDestinationFactory DEFAULT_DESTINATION_FACTORY = new PathDestinationFactory();

    public ClusterEventPublisher(ApplicationEventPublisher applicationEventPublisher, BusProperties busProperties) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.busProperties = busProperties;
        this.clusterEventPublisher = this;
    }

    public static ClusterEventPublisher getInstance() {
        if (clusterEventPublisher == null) {
            throw new NullPointerException("发送器还没有初始化");
        }
        return clusterEventPublisher;
    }


    public String getOriginService() {
        return busProperties.getId();
    }

    public Destination getDestination(String toService) {
        return DEFAULT_DESTINATION_FACTORY.getDestination(toService);
    }


    /**
     * 广播模式的事件
     *
     * @return
     */
    public AbstractClusterEvent createClusterEvent() {
        return new AbstractClusterEvent();
    }

    /**
     * 发送指定集群事件
     *
     * @return
     */
    public AbstractClusterEvent createClusterEvent(String toService) {
        return new AbstractClusterEvent(toService);
    }


    public void publishEvent(AbstractClusterEvent clusterEvent) {
        if (ObjectUtil.isEmpty(clusterEvent.getSendTime())) {
            clusterEvent.setSendTime(DateTime.now());
        }
        applicationEventPublisher.publishEvent(clusterEvent);
    }


}
