package jbm.framework.boot.autoconfigure.mail.test;

import org.junit.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-06 01:56
 **/
public class MailTest {


    @Test
    public void testSend(String[] args) {
        JavaMailSenderImpl js = new JavaMailSenderImpl();
        js.setHost("smtp.qq.com");
        js.setUsername("350006811@qq.com");
        js.setPassword("mygdzwlcjlxpbghc");
        Properties props = new Properties();
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.ssl.enable", true);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.timeout", 25000);
        js.setJavaMailProperties(props);
        MimeMessage message = null;
        try {
            message = js.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            // helper.setFrom(new InternetAddress(this.getFrom(), MimeUtility.encodeText(this.name,"UTF-8", "B")));
            helper.setFrom("350006811@qq.com");
            helper.setTo("350006811@qq.com");
            helper.setSubject("测试");
            helper.setText("测试内容");
            //addAttachmentStatic(helper,params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        js.send(message);
    }
}
