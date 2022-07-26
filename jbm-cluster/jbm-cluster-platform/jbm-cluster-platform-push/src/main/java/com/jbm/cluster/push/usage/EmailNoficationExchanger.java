package com.jbm.cluster.push.usage;

import com.jbm.cluster.api.entitys.message.EmailNotification;
import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.api.model.push.PushCallback;
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
    public PushCallback apply(EmailNotification emailNotification) {
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTitle(emailNotification.getTitle());
        mailRequest.setContent(emailNotification.getContent());
        mailRequest.setMailTo(emailNotification.getReceiver());
        mailSender.sendSimpleMail(mailRequest);
        return this.success(emailNotification);
    }

    @Override
    public EmailNotification build(PushMessageBody pushMessageBody, PushMessageItem pushMessageItem) {
        return null;
    }

}
