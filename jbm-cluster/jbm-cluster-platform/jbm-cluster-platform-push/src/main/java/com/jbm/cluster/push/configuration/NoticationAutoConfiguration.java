package com.jbm.cluster.push.configuration;

import com.jbm.cluster.common.constants.QueueConstants;
import com.jbm.cluster.push.usage.SmsNotificationExchanger;
import jbm.framework.aliyun.sms.AliyunSmsTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author LIQIU
 * @date 2018-3-27
 **/
@Configuration
public class NoticationAutoConfiguration {
//    @Bean
//    public Queue notificationQueue() {
//        return new Queue(QueueConstants.QUEUE_PUSH_MESSAGE);
//    }

    @Bean
    public SmsNotificationExchanger smsNotifcationExchanger(AliyunSmsTemplate aliyunSmsTemplate) {
        return new SmsNotificationExchanger(aliyunSmsTemplate);
    }


}
