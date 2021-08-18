package com.jbm.cluster.push.configuration;

import com.jbm.cluster.api.model.message.MqttNotification;
import com.jbm.cluster.common.constants.QueueConstants;
import com.jbm.cluster.push.usage.EmailNoficationExchanger;
import com.jbm.cluster.push.usage.MqttNotificationExchanger;
import com.jbm.cluster.push.usage.SmsNotificationExchanger;
import jbm.framework.aliyun.sms.AliyunSmsTemplate;
import jbm.framework.boot.autoconfigure.mail.MailSendTemplate;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author LIQIU
 * @date 2018-3-27
 **/
@Configuration
public class NoticationAutoConfiguration {


    @Bean
//    @ConditionalOnBean(AliyunSmsTemplate.class)
    public SmsNotificationExchanger smsNotifcationExchanger(AliyunSmsTemplate aliyunSmsTemplate) {
        return new SmsNotificationExchanger(aliyunSmsTemplate);
    }


    @Bean
//    @ConditionalOnBean(RealMqttPahoClientFactory.class)
    public MqttNotificationExchanger mqttNotificationExchanger(RealMqttPahoClientFactory mqttPahoClientFactory) {
        return new MqttNotificationExchanger(mqttPahoClientFactory);
    }


    @Bean
//    @ConditionalOnBean(MailSendTemplate.class)
    public EmailNoficationExchanger emailNotifcationExchanger(MailSendTemplate mailSendTemplate) {
        return new EmailNoficationExchanger(mailSendTemplate);
    }

}
