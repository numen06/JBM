package com.jbm.cluster.push.service.impl;

import com.jbm.cluster.api.constants.push.MessageSendStatus;
import com.jbm.cluster.api.model.entity.message.PushMessage;
import com.jbm.cluster.node.client.NotificationClient;
import com.jbm.cluster.push.service.PushMessageService;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;


/**
 * @Author: auto generate by jbm
 * @Create: 2020-03-05 03:05:23
 */
@Service
public class PushMessageServiceImpl extends MasterDataServiceImpl<PushMessage> implements PushMessageService {

    @Autowired
    private NotificationClient notificationClient;

    @Override
    public DataPaging<PushMessage> selectPageList(PageRequestBody<PushMessage> pageRequestBody) {
        return super.selectEntitys(pageRequestBody);
    }

    public PushMessage buildDefPushMessage() {
        PushMessage message = new PushMessage();
        message.setSmsStatus(MessageSendStatus.unsent);
        message.setEmailStatus(MessageSendStatus.unsent);
        return message;
    }

    @Override
    public boolean save(PushMessage pushMessage) {
        PushMessage message = buildDefPushMessage();
//        //发送邮件,发送短信
//        if (messageDTO.getSendSms() || messageDTO.getSendEmail()) {
//            sysUserVO = sysUserClient.findUserById(messageDTO.getSysUserId());
//            if (messageDTO.getSendSms()) {
//                message.setSmsStatus(WAIT_SEND_STATUS);
//                Assert.hasText(message.getTemplateCode(), "站内信的参数是发送短信，短信模板不能为空");
//                if (sendSms(messageDTO, sysUserVO)) {
//                    message.setSmsStatus(FINISH_SEND_STATUS);
//                }
//            }
//
//            if (messageDTO.getSendEmail()) {
//                message.setEmailStatus(WAIT_SEND_STATUS);
//                if (sendMail(messageDTO, sysUserVO)) {
//                    message.setEmailStatus(FINISH_SEND_STATUS);
//                }
//            }
//        }
        return super.save(message);
    }


    @Override
    public boolean read(Long id) {
        Assert.notNull(id, "站内信id不能为空");
        PushMessage message = new PushMessage();
        message.setId(id);
        message.setReadFlag(1);
        return super.updateById(message);
    }

//    public boolean sendMail(PushMessage pushMessage) {
//        EmailNotification emailNotification = new EmailNotification();
//        emailNotification.setContent(pushMessage.getContent());
//        emailNotification.setReceiver(sysUserVO.getEmail());
//        emailNotification.setTitle(pushMessage.getTitle());
//        notificationClient.send(emailNotification);
//        //TODO 邮件发送异常未捕获
//        return true;
//    }
//
//    public boolean sendSms(PushMessage messageDTO ) {
//        SmsNotification smsNotification = new SmsNotification();
//        smsNotification.setPhoneNumber(sysUserVO.getPhoneNumber());
//        smsNotification.setSignName(messageDTO.getSignName());
//        smsNotification.setParams(messageDTO.getSmsParams());
//        smsNotification.setTemplateCode(messageDTO.getTemplateCode());
//        notificationClient.send(smsNotification);
//        return true;
//    }

}
