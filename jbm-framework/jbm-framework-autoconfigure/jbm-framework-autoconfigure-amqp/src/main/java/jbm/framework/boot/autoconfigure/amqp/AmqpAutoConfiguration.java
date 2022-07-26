package jbm.framework.boot.autoconfigure.amqp;

import com.rabbitmq.client.Channel;
import jbm.framework.boot.autoconfigure.amqp.usage.FastJsonMessageConverter;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnClass({RabbitTemplate.class, Channel.class})
@EnableConfigurationProperties({RabbitProperties.class})
public class AmqpAutoConfiguration {

    @Autowired
    private ApplicationContext applicationContext;


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
//        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setMessageConverter(new FastJsonMessageConverter());
        return template;
    }

    @Bean(name = "rabbitListenerContainerFactory")
    @Primary
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        //默认消费者数量
        factory.setConcurrentConsumers(3);
        //最大消费者数量
        factory.setMaxConcurrentConsumers(15);
        //每次给消费者发送的消息数量
        factory.setPrefetchCount(1);
//        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
//        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setMessageConverter(new FastJsonMessageConverter());
        return factory;
    }

}
