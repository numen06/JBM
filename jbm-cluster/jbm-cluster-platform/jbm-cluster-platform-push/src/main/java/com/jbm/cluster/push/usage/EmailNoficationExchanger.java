package com.jbm.cluster.push.usage;

import com.jbm.cluster.api.model.message.EmailNotification;
import com.jbm.cluster.api.model.message.Notification;
import jbm.framework.boot.autoconfigure.mail.MailSendTemplate;
import jbm.framework.boot.autoconfigure.mail.model.MailRequest;

/**
 * @author LIQIU
 * @date 2018-3-27
 **/
public class EmailNoficationExchanger implements NotificationExchanger {

    private final MailSendTemplate mailSender;

    public EmailNoficationExchanger(MailSendTemplate mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public boolean support(Object notification) {
        return notification.getClass().equals(EmailNotification.class);
    }

    @Override
    public boolean exchange(Notification notification) {
        EmailNotification emailNotification = (EmailNotification) notification;
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTitle(emailNotification.getTitle());
        mailRequest.setContent(emailNotification.getContent());
        mailRequest.setMailTo(emailNotification.getReceiver());
        mailSender.sendSimpleMail(mailRequest);
        return false;
    }
}
