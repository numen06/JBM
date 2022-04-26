package com.jbm.cluster.logs.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.model.gateway.GatewayLogInfo;
import com.jbm.cluster.common.core.utils.AddressUtils;
import com.jbm.cluster.core.constant.QueueConstants;
import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.cluster.logs.service.GatewayLogsService;
import com.jbm.util.statistics.CountWithTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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

    private CountWithTime countWithTime = new CountWithTime() {


        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.MINUTES);
        }

        @Override
        public void print() {
            log.info("最近1分钟处理日志:{}", this.getAvg());
        }
    };


    @Autowired
    private StreamBridge streamBridge;

    @Bean
    public Function<Flux<Message<GatewayLogs>>, Mono<Void>> accessLogs() {
        return flux -> flux.map(message -> {
            GatewayLogs logs = message.getPayload();
            if (ObjectUtil.isNotEmpty(logs)) {
                if (StrUtil.isNotBlank(logs.getIp())) {
                    logs.setRegion(AddressUtils.getIPRegion(logs.getIp()));
                }
                gatewayLogsService.save(logs);
            }
            return message;
        }).then();
    }

    /**
     * 接收访问日志
     *
     * @param gatewayLogInfoJson
     */
    @RabbitListener(queues = QueueConstants.QUEUE_ACCESS_LOGS)
    public void accessLogsQueue(@Payload String gatewayLogInfoJson) {
        try {
            countWithTime.add();
            if (StrUtil.isNotBlank(gatewayLogInfoJson)) {
                GatewayLogInfo logs = JSON.parseObject(gatewayLogInfoJson, GatewayLogInfo.class);
//                GatewayLogs logs = JSON.parseObject(gatewayLogInfoJson, GatewayLogs.class);
//                if (ObjectUtil.isNotEmpty(logs)) {
//                    if (StrUtil.isNotBlank(logs.getIp())) {
//                        logs.setRegion(AddressUtils.getIPRegion(logs.getIp()));
//                    }
////                    logs.setUseTime(logs.getResponseTime().getTime() - logs.getRequestTime().getTime());
//                    gatewayLogsService.save(logs);
//                }
                streamBridge.send("accessLogs-in-0", logs);
            }
        } catch (Exception e) {
            log.error("日志接收错误:", e);
        }
    }
}
