package com.jbm.cluster.logs.handler;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.cluster.logs.event.AccessEvent;
import com.jbm.cluster.logs.service.GatewayLogsService;
import com.jbm.util.batch.ActionBean;
import com.jbm.util.batch.RollingTask;
import jbm.framework.boot.autoconfigure.ip2region.IpRegionTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
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
public class GatewayLogsHandler {

    /**
     * 临时存放减少io
     */
    @Autowired
    private GatewayLogsService gatewayLogsService;

    private RollingTask countWithTime = RollingTask.createRollingTask( 1L,TimeUnit.MINUTES, new Function<ActionBean<Long>, Long>() {

        @Override
        public Long apply(ActionBean<Long> actionBean) {
            log.info("最近1分钟处理日志:{}", actionBean.getCurrQuantity());
            return actionBean.getObj();
        }
    });


    @Autowired
    private IpRegionTemplate ipRegionTemplate;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Bean
    public Function<Flux<Message<GatewayLogs>>, Mono<Void>> accessLogs() {
        return flux -> flux.map(message -> {
            try {
                countWithTime.offer();
                GatewayLogs logs = message.getPayload();
                //如果日志等级不够1,则不记录

                if (ObjectUtil.isNotEmpty(logs)) {
                    logs.setLoglevel(ObjectUtil.defaultIfNull(logs.getLoglevel(), 0));
                    if (logs.getLoglevel() <= 0) {
                        return message;
                    }
                    int nowMillis = DateTime.now().getField(DateField.MILLISECOND);
                    DateTime time = DateTime.of(logs.getRequestTime()).setField(DateField.MILLISECOND, nowMillis);
                    logs.setRequestTime(time);


                    if (StrUtil.isNotBlank(logs.getIp())) {
                        //设置IP属地
                        logs.setRegion(ipRegionTemplate.getRegion(logs.getIp()));
                    }
                    if (StrUtil.isEmpty(logs.getAccessId())) {
                        //设置IP属地
                        logs.setAccessId(IdUtil.fastSimpleUUID());
                    }
                    applicationEventPublisher.publishEvent(new AccessEvent(this, logs));
//                    gatewayLogsService.save(logs);
                    gatewayLogsService.saveGatewayLogs(logs);
                }
                return message;
            } catch (Exception e) {
                log.error("格式化错误", e);
                return null;
            }
        }).then();
    }

}
