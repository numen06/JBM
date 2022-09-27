package com.jbm.cluster.push.message;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.push.PushMsgType;
import com.jbm.cluster.api.constants.push.PushWay;
import com.jbm.cluster.api.event.TestBusinessEvent;
import com.jbm.cluster.api.model.push.PushMsg;
import com.jbm.cluster.common.basic.module.JbmClusterBusinessEventTemplate;
import com.jbm.cluster.common.basic.module.JbmClusterNotification;
import com.jbm.cluster.push.service.PushMessageBodyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PushMessageTest {

    @Autowired
    private PushMessageBodyService pushMessageBodyService;
    @Autowired
    private JbmClusterNotification jbmClusterNotification;
    /**
     * 引入时间发送模板类
     */
    @Autowired
    private JbmClusterBusinessEventTemplate jbmClusterBusinessEventTemplate;

    /**
     *
     * 发送测试事件
     */
    //    @Scheduled(cron = "0/5 * *  * * ? ")
    public void testSend() {
        PushMsg pushMsg = new PushMsg();
//        pushMsg.setPushWays(Lists.newArrayList(PushWay.wechat));
        pushMsg.setPushWays(Lists.newArrayList(PushWay.mqtt));
        pushMsg.setPushMsgType(PushMsgType.notification);
        pushMsg.setRecUserIds(Lists.newArrayList(0L));
        pushMsg.setTemplateCode("WoGNsFFOxqUJq1E3Mi28evaeQINnaiZAPiXRnC2WgEc");
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("first", "测试");
        objectMap.put("keyword1", "能源物联监控项目");
        objectMap.put("keyword2", "A相电压过电压");
        objectMap.put("keyword3", "预警");
        objectMap.put("keyword4", "低压配电房进线回路");
        objectMap.put("keyword5", DateUtil.now());
        objectMap.put("remark", "请点击详情查看。");
        pushMsg.setExtend(objectMap);
        pushMsg.setSysMsg(true);
        pushMsg.setSendUserId(0L);
        pushMsg.setTitle("测试");
        pushMsg.setContent(StrUtil.format("{}发的:{}", DateTime.now(), pushMsg.getTitle()));
//        pushMessageBodyService.sendPushMsg(pushMsg);
        jbmClusterNotification.pushMsg(pushMsg);
        jbmClusterBusinessEventTemplate.sendBusinessEvent(new TestBusinessEvent("我是测试事件:" + DateUtil.now()));
    }


}

