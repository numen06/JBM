package com.jbm.cluster.push.message;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.push.PushMsgType;
import com.jbm.cluster.api.constants.push.PushWay;
import com.jbm.cluster.api.event.TestBusinessEvent;
import com.jbm.cluster.api.model.push.PushMsg;
import com.jbm.cluster.common.basic.module.JbmBusinessLogTemplate;
import com.jbm.cluster.common.basic.module.JbmClusterBusinessEventTemplate;
import com.jbm.cluster.common.basic.module.JbmClusterNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PushMessageTest {

    @Autowired
    private JbmClusterNotification jbmClusterNotification;

    /**
     * 引入时间发送模板类
     */
    @Autowired
    private JbmClusterBusinessEventTemplate jbmClusterBusinessEventTemplate;
//    @Scheduled(cron = "0/5 * * * * ?")
    public void testLogSend(){
        testMdcLogging();
    }

    public void testMdcLogging() {
        // 1. 使用 logStart 构建上下文并获取 logId
        String logId = JbmBusinessLogTemplate.logStart(builder -> {
            String traceId = IdUtil.fastSimpleUUID();
            builder.traceId(traceId)
                    .source("mdc-logback-demo")
                    .expireDays(7)
                    .autoTimestamp(true);
        });
        // 2. 直接通过常规日志输出，BusinessLogMdcAppender 会自动采集
        log.info("导入任务启动");
        log.info("准备导入文件: {}", "customer.xlsx");
        log.info("读取文件成功，开始解析...");
        try {
            log.info("数据解析完成，正在写入数据库...");
            log.info("导入任务成功，耗时 3.2s");
        } catch (Exception ex) {
            log.error("导入任务失败", ex);
        } finally {
            log.info("MDC 日志采集流程结束 logId={}", logId);
            // 通知结束并清理上下文
            JbmBusinessLogTemplate.logEnd(logId);
        }
    }
    /**
     *
     * 发送测试事件
     */
//        @Scheduled(cron = "0/5 * *  * * ? ")
    public void testSendPush() {
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

    }
    @Scheduled(cron = "0/5 * *  * * ? ")
    public void testSendEvent() {
        //判断当前profile是jaja则发送测试
        if (StrUtil.equals("jaja", System.getProperty("spring.profiles.active"))) {
            jbmClusterBusinessEventTemplate.sendBusinessEvent(new TestBusinessEvent("我是测试事件:" + DateUtil.now()));
        }
    }


}

