package jbm.framework.boot.autoconfigure.mail.configuration;

import jbm.framework.boot.autoconfigure.mail.MailSendTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-06 01:33
 **/
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class MailSenderConfiguration {

    @Bean
    public MailSendTemplate mailSendTemplate(JavaMailSender javaMailSender, MailProperties mailProperties) {
        MailSendTemplate mailSendTemplate = new MailSendTemplate(javaMailSender, mailProperties);
        return mailSendTemplate;
    }
}
