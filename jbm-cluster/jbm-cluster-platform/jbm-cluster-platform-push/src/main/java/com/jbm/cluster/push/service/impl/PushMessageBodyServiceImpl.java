package com.jbm.cluster.push.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.push.PushMsgType;
import com.jbm.cluster.api.constants.push.PushWay;
import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.api.model.push.PushMessageResult;
import com.jbm.cluster.api.model.push.PushMsg;
import com.jbm.cluster.push.form.PushMessageForm;
import com.jbm.cluster.push.handler.NotificationDispatcher;
import com.jbm.cluster.push.service.PushRecipientResolver;
import com.jbm.cluster.push.service.PushMessageBodyService;
import com.jbm.cluster.push.service.PushMessageItemService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-07-21 16:47:16
 */
@Slf4j
@Service
public class PushMessageBodyServiceImpl extends MasterDataServiceImpl<PushMessageBody> implements PushMessageBodyService {
    @Autowired
    private NotificationDispatcher notificationDispatcher;
    @Autowired
    private PushMessageItemService pushMessageItemService;
    @Autowired
    private PushRecipientResolver pushRecipientResolver;

    @Override
    public DataPaging<PushMessageBody> selectPageList(PageRequestBody pageRequestBody) {
        return super.selectEntitys(pageRequestBody);
    }


    @Override
    public DataPaging<PushMessageResult> selectPushMessagePageList(PushMessageForm pushMessageform) {
        DataPaging<PushMessageItem> pushMessageItemDataPaging = pushMessageItemService.pageList(pushMessageform);
        List<PushMessageResult> pushMessageBodyList = buildPushMessage(pushMessageItemDataPaging.getContents());
        return new DataPaging<>(pushMessageBodyList, pushMessageItemDataPaging);
    }

    public List<PushMessageResult> buildPushMessage(List<PushMessageItem> pushMessageItems){
        List<PushMessageResult> pushMessageBodyList = Lists.newArrayList();
        if (ObjectUtil.isEmpty(pushMessageItems)) {
            return pushMessageBodyList;
        }
        List<Long> msgBodyIds = pushMessageItems.stream()
                .map(PushMessageItem::getMsgBodyId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, PushMessageBody> bodyMap = msgBodyIds.isEmpty() ? Collections.emptyMap() : listMessageBodies(msgBodyIds)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PushMessageBody::getId, Function.identity(), (left, right) -> left));
        pushMessageItems.forEach(pushMessageItem -> {
            PushMessageResult pushMessageResult = new PushMessageResult();
            BeanUtil.copyProperties(pushMessageItem, pushMessageResult);
            PushMessageBody pushMessageBody = pushMessageItem.getMsgBodyId() == null ? null : bodyMap.get(pushMessageItem.getMsgBodyId());
            if (pushMessageBody == null) {
                log.warn("Push message body missing, msgId={}, msgBodyId={}", pushMessageItem.getMsgId(), pushMessageItem.getMsgBodyId());
            } else {
                BeanUtil.copyProperties(pushMessageBody, pushMessageResult);
            }
            pushMessageResult.setSysMsg(pushMessageResult.getSendUserId() == null);
            pushMessageBodyList.add(pushMessageResult);
        });
        return pushMessageBodyList;
    }

    private List<PushMessageBody> listMessageBodies(List<Long> msgBodyIds) {
        QueryWrapper<PushMessageBody> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "send_user_id", "title", "tags", "content", "template_code", "type", "level", "url", "create_time", "update_time");
        queryWrapper.in("id", msgBodyIds);
        return list(queryWrapper);
    }

    @Override
    public DataPaging<PushMessageResult> findUserPushMessage(PushMessageForm pushMessageform) {
        DataPaging<PushMessageItem> pushMessageItemDataPaging = pushMessageItemService.findUserPushMessage(pushMessageform);
        List<PushMessageResult> pushMessageBodyList = buildPushMessage(pushMessageItemDataPaging.getContents());
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

    @Resource
    private PushMessageBodyService self;

    @Override
    public void sendPushMsg(PushMsg pushMsg) {
        if (ObjectUtil.isEmpty(pushMsg)) {
            throw new ServiceException("推送消息不能为空");
        }
        if (ObjectUtil.isEmpty(pushMsg.getPushWays())) {
            pushMsg.setPushWays(Lists.newArrayList(PushWay.internal));
        }
        if (ObjectUtil.isEmpty(pushMsg.getTitle())) {
            throw new ServiceException("请指定推送标题");
        }
        if (ObjectUtil.isEmpty(pushMsg.getContent())) {
            throw new ServiceException("请指定推送内容");
        }
        Set<Long> recUserIds = pushRecipientResolver.resolve(pushMsg);
        if (ObjectUtil.isEmpty(recUserIds)) {
            throw new ServiceException("用户站内信请指定接收者，标签组");
        }
        if (BooleanUtil.isFalse(pushMsg.getSysMsg())) {
            if (ObjectUtil.isEmpty(pushMsg.getSendUserId())) {
                throw new ServiceException("请指定接收者");
            }
        }
        PushMessageBody pushMessageBody = new PushMessageBody();
        pushMessageBody.setSendUserId(pushMsg.getSendUserId());
        pushMessageBody.setTitle(pushMsg.getTitle());
        pushMessageBody.setType(ObjectUtil.defaultIfNull(pushMsg.getPushMsgType(), PushMsgType.notification));
        pushMessageBody.setContent(pushMsg.getContent());
        pushMessageBody.setExtend(pushMsg.getExtend());
        pushMessageBody.setTemplateCode(pushMsg.getTemplateCode());
        pushMessageBody.setUrl(pushMsg.getUrl());
        pushMessageBody.setTags(pushMsg.getTags());
        pushMessageBody.setLevel(pushMsg.getLevel());
        self.saveEntity(pushMessageBody);
        List<Long> targetUserIds = new ArrayList<>(recUserIds);
        targetUserIds.forEach(recUserId -> pushMsg.getPushWays().forEach(pushWay -> pushMessageItemService.toPush(pushWay, pushMessageBody, recUserId)));
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
        self.save(pushMessageBody);
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
