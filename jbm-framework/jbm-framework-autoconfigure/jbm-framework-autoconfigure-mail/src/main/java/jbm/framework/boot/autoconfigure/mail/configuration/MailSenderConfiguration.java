package jbm.framework.boot.autoconfigure.mail.configuration;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailAccount;
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

    @Bean
    public MailAccount mailAccount(MailProperties mailProperties) {
        MailAccount account = new MailAccount();
        account.setHost(mailProperties.getHost());
        account.setPort(mailProperties.getPort());
        account.setAuth(true);
        String from = mailProperties.getUsername();
        String username = StrUtil.subBefore(from, "@", false);
        account.setFrom(from);
        account.setUser(username);
        account.setPass(mailProperties.getPassword());
        if (account.getPort() != 25) {
            account.setSslEnable(true);
        }
        return account;
    }
}
