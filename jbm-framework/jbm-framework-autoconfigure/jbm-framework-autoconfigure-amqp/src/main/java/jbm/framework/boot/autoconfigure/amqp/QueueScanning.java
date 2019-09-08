package jbm.framework.boot.autoconfigure.amqp;

import com.google.common.collect.Maps;
import jbm.framework.boot.autoconfigure.amqp.usage.AbstractAmqpSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.context.ApplicationContext;

import java.util.Map;

@Slf4j
public class QueueScanning {

    private final ApplicationContext applicationContext;

    public QueueScanning(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private final Map<String, Queue> queues = Maps.newHashMap();

    public void scanning() {
        Map<String, AbstractAmqpSender> abstractAmqpSenderMap = applicationContext.getBeansOfType(AbstractAmqpSender.class);
        for (String key : abstractAmqpSenderMap.keySet()) {
            AbstractAmqpSender abstractAmqpSender = abstractAmqpSenderMap.get(key);
            queues.put(abstractAmqpSender.queue().getName(), abstractAmqpSender.queue());
        }
//        DefaultListableBeanFactory fty = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
//        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(applicationContext);
        for (Queue queue : queues.values()) {
            log.info("扫描到队列:{}", queue.getName());
        }
    }

}
