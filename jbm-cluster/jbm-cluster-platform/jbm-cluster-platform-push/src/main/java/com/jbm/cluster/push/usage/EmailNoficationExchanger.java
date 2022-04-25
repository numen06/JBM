package com.jbm.cluster.push.usage;

import com.jbm.cluster.api.model.entitys.message.EmailNotification;
import com.jbm.cluster.api.model.entitys.message.Notification;
import jbm.framework.boot.autoconfigure.mail.MailSendTemplate;
import jbm.framework.boot.autoconfigure.mail.model.MailRequest;

/**
 * @author LIQIU
 * @date 2018-3-27
 **/
public class EmailNoficationExchanger extends BaseNotificationExchanger<EmailNotification> {

    private final MailSendTemplate mailSender;

    public EmailNoficationExchanger(MailSendTemplate mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public boolean process(EmailNotification emailNotification) {
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTitle(emailNotification.getTitle());
        mailRequest.setContent(emailNotification.getContent());
        mailRequest.setMailTo(emailNotification.getReceiver());
        mailSender.sendSimpleMail(mailRequest);
        return false;
    }
}
