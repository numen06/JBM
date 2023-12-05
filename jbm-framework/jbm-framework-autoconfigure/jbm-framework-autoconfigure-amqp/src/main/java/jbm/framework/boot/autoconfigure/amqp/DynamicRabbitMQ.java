package jbm.framework.boot.autoconfigure.amqp;

import cn.hutool.core.util.StrUtil;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.context.ApplicationContext;

public class DynamicRabbitMQ {


    private final RabbitTemplate rabbitTemplate;

    private final ApplicationContext applicationContext;

    public DynamicRabbitMQ(RabbitTemplate rabbitTemplate, ApplicationContext applicationContext) {
        this.rabbitTemplate = rabbitTemplate;
        this.applicationContext = applicationContext;
    }


    public SimpleMessageListenerContainer createMessageListenerContainer(ChannelAwareMessageListener channelAwareMessageListener, String... queueNames) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(rabbitTemplate.getConnectionFactory());
        //同时监听多个队列
        container.setQueueNames(queueNames);
        //设置当前的消费者数量
        container.setConcurrentConsumers(1);
        container.setMaxConcurrentConsumers(5);
        //设置是否重回队列
        container.setDefaultRequeueRejected(false);
        //设置自动签收
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        //设置监听外露
        container.setExposeListenerChannel(true);
        //设置消费端标签策略
//        container.setConsumerTagStrategy(new ConsumerTagStrategy() {
//            @Override
//            public String createConsumerTag(String queue) {
//                return queue + "_" + UUID.randomUUID().toString();
//            }
//        });
        //设置消息监听
        container.setMessageListener(channelAwareMessageListener);
        //动态初始化对象
        applicationContext.getAutowireCapableBeanFactory().initializeBean(container, StrUtil.format("{}-{}", DynamicRabbitMQ.class.getSimpleName(), StrUtil.join("-", queueNames)));
        return container;
    }

}
