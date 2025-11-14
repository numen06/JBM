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
import org.slf4j.MDC;
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
    // @Scheduled(cron = "0/5 * * * * ?")
    public void testLogSend(){
        testMdcLogging();
    }

    public void testMdcLogging() {
        // 1. 构建 MDC：traceId + 业务信息
        String traceId = IdUtil.fastSimpleUUID();
        MDC.put("traceId", traceId);
        JbmBusinessLogTemplate.BusinessLogMdc.bindBusiness("DATA_IMPORT", "TASK-" + traceId.substring(0, 8));
        JbmBusinessLogTemplate.BusinessLogMdc.bindSource("mdc-logback-demo");
        JbmBusinessLogTemplate.BusinessLogMdc.overrideExpireDays(7);
        JbmBusinessLogTemplate.BusinessLogMdc.enableAutoTimestamp();

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
            // 通知 Appender 结束，可释放上下文
            JbmBusinessLogTemplate.BusinessLogMdc.markFinished();
            // 3. 清理 MDC，避免污染线程
            JbmBusinessLogTemplate.BusinessLogMdc.clearBusinessKeys();
            MDC.remove("traceId");
            log.info("MDC 日志采集流程结束 traceId={}", traceId);
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

