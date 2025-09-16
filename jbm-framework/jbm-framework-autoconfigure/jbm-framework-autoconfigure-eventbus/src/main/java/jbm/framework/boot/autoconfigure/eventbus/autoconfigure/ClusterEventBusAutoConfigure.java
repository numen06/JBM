package jbm.framework.boot.autoconfigure.eventbus.autoconfigure;

import jbm.framework.boot.autoconfigure.eventbus.ClusterBusConsumer;
import jbm.framework.boot.autoconfigure.eventbus.ClusterEventListener;
import jbm.framework.boot.autoconfigure.eventbus.publisher.ClusterEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.bus.*;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.springframework.cloud.bus.BusConstants.BUS_CONSUMER;

/**
 * 集群事件配置类
 */
@ConditionalOnBusEnabled
@EnableConfigurationProperties(BusProperties.class)
public class ClusterEventBusAutoConfigure {


    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private BusProperties busProperties;

    @Bean
    public ClusterEventListener clusterEventListener() {
        return new ClusterEventListener();
    }

    @Bean
    public ClusterEventPublisher eventPublisher() {
        return new ClusterEventPublisher(applicationEventPublisher, busProperties);
    }

    /**
     * 全局事件异步操作
     * @return
     */
    @Bean
    public ApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster =
                new SimpleApplicationEventMulticaster();
        // 使用自定义线程池
        eventMulticaster.setTaskExecutor(taskExecutor());
        return eventMulticaster;
    }

    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(20);
        // 最大线程数
        executor.setMaxPoolSize(50);
        // 队列容量（避免太小）
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("spring-event-");
        executor.initialize();
        return executor;
    }

    /**
     * 替换原有的接收器
     *
     * @param applicationEventPublisher
     * @param serviceMatcher
     * @param busBridge
     * @param properties
     * @param destinationFactory
     * @return
     */
    @Bean(BUS_CONSUMER)
    public BusConsumer busConsumer(ApplicationEventPublisher applicationEventPublisher, ServiceMatcher serviceMatcher,
                                   ObjectProvider<BusBridge> busBridge, BusProperties properties, Destination.Factory destinationFactory) {
        return new ClusterBusConsumer(applicationEventPublisher, serviceMatcher, busBridge, properties, destinationFactory);
    }


}
