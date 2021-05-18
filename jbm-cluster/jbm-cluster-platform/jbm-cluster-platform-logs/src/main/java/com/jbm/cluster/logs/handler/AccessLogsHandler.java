package com.jbm.cluster.logs.handler;

import cn.hutool.core.bean.BeanUtil;
import com.jbm.cluster.common.constants.QueueConstants;
import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.cluster.logs.service.GatewayLogsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
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
     * @param access
     */
    @RabbitListener(queues = QueueConstants.QUEUE_ACCESS_LOGS)
    public void accessLogsQueue(@Payload Map access) {
        try {
            if (access != null) {
                GatewayLogs logs = BeanUtil.mapToBean(access, GatewayLogs.class, true);
                if (logs != null) {
                    if (logs.getIp() != null) {
                        logs.setRegion(logs.getIp());
                    }
                    logs.setUseTime(logs.getResponseTime().getTime() - logs.getRequestTime().getTime());
                    gatewayLogsService.save(logs);
                }
            }
        } catch (Exception e) {
            log.error("error:", e);
        }
    }
}
