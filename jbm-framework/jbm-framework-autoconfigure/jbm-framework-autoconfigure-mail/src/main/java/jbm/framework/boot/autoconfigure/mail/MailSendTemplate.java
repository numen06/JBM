package jbm.framework.boot.autoconfigure.mail;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.template.engine.beetl.BeetlUtil;
import jbm.framework.boot.autoconfigure.mail.model.MailRequest;
import lombok.extern.slf4j.Slf4j;
import org.beetl.core.Template;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-06 01:41
 **/
@Slf4j
public class MailSendTemplate {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    public MailSendTemplate(JavaMailSender javaMailSender, MailProperties mailProperties) {
        this.javaMailSender = javaMailSender;
        this.mailProperties = mailProperties;
    }

    public MimeMessage buildNewMail(MailRequest mailRequest) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        if (ObjectUtil.isEmpty(mailRequest.getMailFrom()))
            helper.setFrom(mailProperties.getUsername());
        else
            helper.setFrom(new InternetAddress(mailRequest.getMailFrom(), MimeUtility.encodeText(mailRequest.getName(), "UTF-8", "B")));
        helper.setTo(mailRequest.getMailTo());
        helper.setSubject(mailRequest.getTitle());
        helper.setText(mailRequest.getContent(), true);
        this.addAttachment(helper, mailRequest);
        return message;
    }

    /**
     * 发送邮件
     */
    public void sendSimpleMail(MailRequest mailRequest) {
        try {
            MimeMessage message = buildNewMail(mailRequest);
            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("发送邮箱异常", e);
            throw new RuntimeException("发送邮件异常! from: " + mailRequest.getName() + "! to: " + mailRequest.getMailTo());
        }
    }

    /**
     * 发送html邮件
     */
    public void sendHtmlMail(MailRequest mailRequest) {
        try {
            MimeMessage message = buildNewMail(mailRequest);
            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("发送邮件异常! from: " + mailRequest.getName() + "! to: " + mailRequest.getMailTo());
        }
    }

    /**
     * 发送带附件的邮件
     */
    public void sendAttachmentMail(MailRequest mailRequest) {
        try {
            MimeMessage message = buildNewMail(mailRequest);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("发送邮件异常! from: " + mailRequest.getName() + "! to: " + mailRequest.getMailTo());
        }
    }

    /**
     * 发送模板邮件
     */
    public void sendTemplateMail(MailRequest mailRequest) {
        try {
            MimeMessage message = this.buildNewMail(mailRequest);
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            Template template = BeetlUtil.getFileTemplate("mail", mailRequest.getTemplateFile());
            String html = template.render();
            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("发送邮件异常! from: " + mailRequest.getName() + "! to: " + mailRequest.getMailTo());
        }
    }

    /**
     * 添加附件
     *
     * @param helper
     * @param params
     * @throws MessagingException
     */
    private void addAttachment(MimeMessageHelper helper, MailRequest params) throws MessagingException {
        if (params.getAttachments() != null) {
            List<File> attachments = params.getAttachments();
            for (File file : attachments) {
                FileSystemResource attachment = new FileSystemResource(file);
                helper.addAttachment(file.getName(), file);
            }
        }
    }


}
