package com.jbm.cluster.push.configuration;

import com.jbm.cluster.push.usage.*;
import com.jbm.cluster.common.basic.module.JbmClusterNotification;
import jbm.framework.aliyun.sms.AliyunSmsTemplate;
import jbm.framework.boot.autoconfigure.mail.MailSendTemplate;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wesley.zhang
 * @date 2018-3-27
 **/
@Configuration
public class NoticationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JbmClusterNotification jbmClusterNotification() {
        return new JbmClusterNotification();
    }

    @Bean
    @ConditionalOnBean(AliyunSmsTemplate.class)
    public SmsNotificationExchanger smsNotifcationExchanger(AliyunSmsTemplate aliyunSmsTemplate) {
        return new SmsNotificationExchanger(aliyunSmsTemplate);
    }


    @Bean
    public PushMessageNotificationExchanger pushMessageNotificationExchanger() {
        return new PushMessageNotificationExchanger();
    }


    @Configuration
    @ConditionalOnBean(RealMqttPahoClientFactory.class)
    static class MqttNotificationConfiguration {
        @Bean
        public MqttNotificationExchanger mqttNotificationExchanger(RealMqttPahoClientFactory mqttPahoClientFactory) {
            return new MqttNotificationExchanger(mqttPahoClientFactory);
        }
    }

    @Bean
    @ConditionalOnBean(MailSendTemplate.class)
    public EmailNoficationExchanger emailNotifcationExchanger(MailSendTemplate mailSendTemplate) {
        return new EmailNoficationExchanger(mailSendTemplate);
    }


    @Bean
//    @ConditionalOnBean(MailSendTemplate.class)
    public WeixinNoficationExchanger weixinNotifcationExchanger() {
        return new WeixinNoficationExchanger();
    }


}
