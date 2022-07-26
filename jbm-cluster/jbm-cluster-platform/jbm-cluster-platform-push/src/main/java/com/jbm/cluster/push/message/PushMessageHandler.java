package com.jbm.cluster.push.message;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.push.PushMsgType;
import com.jbm.cluster.api.constants.push.PushWay;
import com.jbm.cluster.api.model.push.PushMsg;
import com.jbm.cluster.push.service.PushMessageBodyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PushMessageHandler {

    @Autowired
    private PushMessageBodyService pushMessageBodyService;

    @Scheduled(cron = "0/5 * *  * * ? ")
    public void testSend() {
        PushMsg pushMsg = new PushMsg();
        pushMsg.setPushWays(Lists.newArrayList(PushWay.mqtt));
        pushMsg.setPushMsgType(PushMsgType.notification);
        pushMsg.setRecUserIds(Lists.newArrayList(0L));
        pushMsg.setSysMsg(true);
        pushMsg.setSendUserId(0L);
        pushMsg.setTitle("测试");
        pushMsg.setContent(StrUtil.format("{}发的:{}", DateTime.now(), pushMsg.getTitle()));
        pushMessageBodyService.sendPushMsg(pushMsg);
    }


}

