package com.jbm.cluster.push.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.push.PushMsgType;
import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.api.model.push.PushMessageResult;
import com.jbm.cluster.api.model.push.PushMsg;
import com.jbm.cluster.push.form.PushMessageForm;
import com.jbm.cluster.push.handler.NotificationDispatcher;
import com.jbm.cluster.push.service.PushMessageBodyService;
import com.jbm.cluster.push.service.PushMessageItemService;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-07-21 16:47:16
 */
@Service
public class PushMessageBodyServiceImpl extends MasterDataServiceImpl<PushMessageBody> implements PushMessageBodyService {
    @Autowired
    private NotificationDispatcher notificationDispatcher;
    @Autowired
    private PushMessageItemService pushMessageItemService;

    @Override
    public DataPaging<PushMessageBody> selectPageList(PageRequestBody pageRequestBody) {
        return super.selectEntitys(pageRequestBody);
    }

    @Override
    public DataPaging<PushMessageResult> findUserPushMessage(PushMessageForm pushMessageform) {
        DataPaging<PushMessageItem> pushMessageItemDataPaging = pushMessageItemService.findUserPushMessage(pushMessageform);
        List<PushMessageResult> pushMessageBodyList = Lists.newArrayList();
        pushMessageItemDataPaging.getContents().forEach(new Consumer<PushMessageItem>() {
            @Override
            public void accept(PushMessageItem pushMessageItem) {
                PushMessageResult pushMessageResult = new PushMessageResult();
                PushMessageBody pushMessageBody = selectById(pushMessageItem.getMsgBodyId());
                BeanUtil.copyProperties(pushMessageItem, pushMessageResult);
                BeanUtil.copyProperties(pushMessageBody, pushMessageResult);
                pushMessageBodyList.add(pushMessageResult);
            }
        });
        return new DataPaging<>(pushMessageBodyList, pushMessageItemDataPaging);

    }

    @Override
    public boolean save(PushMessageBody pushMessageBody) {
//        PushMessage message = buildDefPushMessage();
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
        return super.save(pushMessageBody);
    }


    @Override
    public void sendPushMsg(PushMsg pushMsg) {
        PushMessageBody pushMessageBody = new PushMessageBody();
        pushMessageBody.setSendUserId(pushMsg.getSendUserId());
        pushMessageBody.setTitle(pushMsg.getTitle());
        pushMessageBody.setType(pushMsg.getPushMsgType());
        pushMessageBody.setContent(pushMsg.getContent());
        pushMessageBody.setExtend(pushMsg.getExtend());
        this.saveEntity(pushMessageBody);
        pushMsg.getRecUserIds().forEach(recUserId -> pushMsg.getPushWays().forEach(pushWay -> pushMessageItemService.toPush(pushWay, pushMessageBody, recUserId)));
    }


    @Override
    public void sendSysMessage(PushMessageBody pushMessageBody) {
        pushMessageBody.setId(null);
        pushMessageBody.setType(PushMsgType.notification);
        if (ObjectUtil.isEmpty(pushMessageBody.getLevel())) {
            pushMessageBody.setLevel(1);
        }
        pushMessageBody.setSendUserId(null);
//        pushMessageBody.setReadFlag(false);
        this.save(pushMessageBody);
//        dispatcher.dispatch( pushMessage);
    }

    @Override
    public void sendUserMessage(PushMessageBody pushMessageBody) {
        pushMessageBody.setId(null);
        pushMessageBody.setType(PushMsgType.notification);
        pushMessageBody.setLevel(1);
//        pushMessageBody.setReadFlag(0);
        this.save(pushMessageBody);
//        dispatcher.dispatch( pushMessage);

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