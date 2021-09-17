package com.jbm.cluster.logs.handler;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.model.GatewayLogInfo;
import com.jbm.cluster.common.constants.QueueConstants;
import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.cluster.logs.service.GatewayLogsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * mq消息接收者
 *
 * @author wesley.zhang
 */
@Service
@Slf4j
public class AccessLogsHandler {

    /**
     * 临时存放减少io
     */
    @Autowired
    private GatewayLogsService gatewayLogsService;

    /**
     * 接收访问日志
     *
     * @param gatewayLogInfoJson
     */
    @RabbitListener(queues = QueueConstants.QUEUE_ACCESS_LOGS)
    public void accessLogsQueue(@Payload String gatewayLogInfoJson) {
        try {
            if (StrUtil.isNotBlank(gatewayLogInfoJson) ) {
                GatewayLogs logs = JSON.parseObject(gatewayLogInfoJson,GatewayLogs.class);
                if (ObjectUtil.isEmpty(logs) ) {
                    if (logs.getIp() != null) {
                        logs.setRegion(logs.getIp());
                    }
//                    logs.setUseTime(logs.getResponseTime().getTime() - logs.getRequestTime().getTime());
                    gatewayLogsService.save(logs);
                }
            }
        } catch (Exception e) {
            log.error("error:", e);
        }
    }
}
