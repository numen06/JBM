package jbm.framework.boot.autoconfigure.eventbus.model;

import jbm.framework.boot.autoconfigure.eventbus.publisher.ClusterEventPublisher;
import lombok.Data;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

import java.util.Date;

/**
 * @author wesley.zhang
 * @create 2021/7/21 10:08 上午
 * @description
 */
@Data
public class AbstractClusterEvent extends RemoteApplicationEvent {

    /**
     * 发送时间
     */
    private Date sendTime;

    /**
     * 接收时间
     */
    private Date reviceTime;

    /**
     * 事件分组
     */
    private String group;

    /**
     * 默认广播模式
     */
    public AbstractClusterEvent() {
        super(AbstractClusterEvent.class, ClusterEventPublisher.getInstance().getOriginService(), ClusterEventPublisher.getInstance().getDestination(null));
    }

    /**
     * 定向发送
     */
    public AbstractClusterEvent(String toService) {
        super(AbstractClusterEvent.class, ClusterEventPublisher.getInstance().getOriginService(), ClusterEventPublisher.getInstance().getDestination(toService));
    }


}
