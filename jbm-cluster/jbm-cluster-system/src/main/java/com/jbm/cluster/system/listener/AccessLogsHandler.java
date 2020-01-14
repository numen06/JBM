package com.jbm.cluster.system.listener;

import cn.hutool.core.bean.BeanUtil;
import com.jbm.cluster.api.model.entity.GatewayAccessLogs;
import com.jbm.cluster.common.constants.QueueConstants;
import com.jbm.cluster.system.mapper.GatewayLogsMapper;
import com.jbm.cluster.system.service.IpRegionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.Map;

/**
 * mq消息接收者
 *
 * @author liuyadu
 */
@Configuration
@Slf4j
public class AccessLogsHandler {

    @Autowired
    private GatewayLogsMapper gatewayLogsMapper;

    /**
     * 临时存放减少io
     */
    @Autowired
    private IpRegionService ipRegionService;

    /**
     * 接收访问日志
     *
     * @param access
     */
    @RabbitListener(queues = QueueConstants.QUEUE_ACCESS_LOGS)
    public void accessLogsQueue(@Payload Map access) {
        try {
            if (access != null) {
                GatewayAccessLogs logs = BeanUtil.mapToBean(access, GatewayAccessLogs.class, true);
                if (logs != null) {
                    if (logs.getIp() != null) {
                        logs.setRegion(ipRegionService.getRegion(logs.getIp()));
                    }
                    logs.setUseTime(logs.getResponseTime().getTime() - logs.getRequestTime().getTime());
                    gatewayLogsMapper.insert(logs);
                }
            }
        } catch (Exception e) {
            log.error("error:", e);
        }
    }
}
