package jbm.framework.boot.autoconfigure.amqp.usage;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class AbstractAmqpSender {
    @Autowired
    protected AmqpTemplate amqpTemplate;

    @Bean
    public abstract Queue queue();


}
