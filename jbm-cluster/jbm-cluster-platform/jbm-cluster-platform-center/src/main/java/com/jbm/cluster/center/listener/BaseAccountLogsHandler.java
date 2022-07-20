package com.jbm.cluster.center.listener;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseAccountLogs;
import com.jbm.cluster.center.service.BaseAccountLogsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @Created wesley.zhang
 * @Date 2022/6/2 12:53
 * @Description TODO
 */
@Configuration
public class BaseAccountLogsHandler {

    @Autowired
    private BaseAccountLogsService baseAccountLogsService;

    @Bean
    public Function<Flux<Message<BaseAccountLogs>>, Mono<Void>> accountLogs() {
        return flux -> flux.map(message -> {
            BaseAccountLogs baseAccountLogs = message.getPayload();
            baseAccountLogsService.insertEntity(baseAccountLogs);
            return message;
        }).then();
    }

}
