package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.model.message.EmailNotification;
import com.jbm.cluster.api.model.message.SmsNotification;
import com.jbm.cluster.push.handler.NotificationDispatcher;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author LIQIU
 * @date 2018-3-27
 **/
@RestController("/notification")
@Api(value = "notification", tags = "通知中心")
public class NotificationController {

    @Autowired
    private NotificationDispatcher dispatcher;

    @ApiOperation("/短信通知")
    @PostMapping("/send/sms")
    public ResultBody<String> sendSms(SmsNotification smsNotification) {
        this.dispatcher.dispatch(smsNotification);
        return ResultBody.ok();
    }

    @ApiOperation("/邮件通知")
    @PostMapping("/send/email")
    public ResultBody<String> sendEmail(EmailNotification emailNotification) {
        this.dispatcher.dispatch(emailNotification);
        return ResultBody.ok();
    }


}
