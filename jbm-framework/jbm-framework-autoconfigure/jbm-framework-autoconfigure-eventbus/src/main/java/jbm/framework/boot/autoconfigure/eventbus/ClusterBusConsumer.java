package jbm.framework.boot.autoconfigure.eventbus;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ReflectUtil;
import jbm.framework.boot.autoconfigure.eventbus.model.AbstractClusterEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.bus.BusBridge;
import org.springframework.cloud.bus.BusConsumer;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @Created wesley.zhang
 * @Date 2022/4/24 22:01
 * @Description TODO
 */
public class ClusterBusConsumer extends BusConsumer {
    public ClusterBusConsumer(ApplicationEventPublisher publisher, ServiceMatcher serviceMatcher, ObjectProvider<BusBridge> busBridge, BusProperties properties, Destination.Factory destinationFactory) {
        super(publisher, serviceMatcher, busBridge, properties, destinationFactory);
    }

    @Override
    public void accept(RemoteApplicationEvent event) {
        if (event instanceof AbstractClusterEvent) {
            ReflectUtil.setFieldValue(event, "reviceTime", DateTime.now());
        }
        super.accept(event);
    }
}
