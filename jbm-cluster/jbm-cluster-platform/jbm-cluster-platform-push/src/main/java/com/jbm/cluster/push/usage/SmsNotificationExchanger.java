package com.jbm.cluster.push.usage;

import cn.hutool.http.HttpException;
import com.alibaba.fastjson.JSONObject;
import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.api.entitys.message.SmsNotification;
import com.jbm.cluster.api.model.push.PushCallback;
import jbm.framework.aliyun.sms.AliyunSmsTemplate;
import jbm.framework.aliyun.sms.model.AliyunSms;
import jbm.framework.aliyun.sms.model.SmsSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

/**
 * 短信通知转发器，可以配置<b>cola.notification.sms.sign-name</b>设置默认的短信签名
 *
 * @author wesley.zhang
 * @date 2018-3-27
 **/
@Slf4j
public class SmsNotificationExchanger extends BaseNotificationExchanger<SmsNotification> {

    private final static String STATUS_OK = "OK";
    private AliyunSmsTemplate aliyunSmsTemplate;

    public SmsNotificationExchanger(AliyunSmsTemplate aliyunSmsTemplate) {
        if (aliyunSmsTemplate != null) {
            log.info("初始化短信通知组件");
        }
        this.aliyunSmsTemplate = aliyunSmsTemplate;
    }


    @Override
    public PushCallback apply(SmsNotification notification) {
        Assert.notNull(aliyunSmsTemplate, "短信接口没有初始化");
        SmsNotification smsNotification = (SmsNotification) notification;
        AliyunSms aliyunSms = new AliyunSms();
        aliyunSms.setTemplateCode(smsNotification.getTemplateCode());
        aliyunSms.getPhoneNumbers().add(((SmsNotification) notification).getPhoneNumber());
        aliyunSms.setTemplateParam(smsNotification.getParams());
        SmsSendResult smsSendResult = null;
        try {
            JSONObject jsonObject = aliyunSmsTemplate.sendSms(aliyunSms);
            smsSendResult = jsonObject.toJavaObject(SmsSendResult.class);
        } catch (Exception e) {
            log.error("发送短信错误", e);
        }
        if (!STATUS_OK.equals(smsSendResult.getCode())) {
            throw new HttpException("发送短信错误");
        }
        return this.success(notification);
    }

    @Override
    public SmsNotification build(PushMessageBody pushMessageBody, PushMessageItem pushMessageItem) {
        return null;
    }

}
