package jbm.framework.aliyun.sms.autoconfigure;

import jbm.framework.aliyun.sms.AliyunSmsTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;


/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-05 05:17
 **/
@EnableConfigurationProperties({AliyunSmsProperties.class})
@ConditionalOnProperty(prefix = "aliyun.sms", name = "access-key-id")
public class AliyunSmsAutoConfiguration {

    @Autowired
    private AliyunSmsProperties aliyunSmsProperties;

    @Bean
    public AliyunSmsTemplate aliyunSmsTemplate() {
        return new AliyunSmsTemplate(aliyunSmsProperties);
    }
}
