package com.jbm.test;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import io.opentelemetry.api.OpenTelemetry;
import jbm.framework.boot.autoconfigure.openobserve.LogPolling;
import jbm.framework.boot.autoconfigure.openobserve.OpenLoggerProvider;
import jbm.framework.boot.autoconfigure.openobserve.OpenObserveTemplate;
import jbm.framework.boot.autoconfigure.openobserve.model.QueryBean;
import jbm.framework.boot.autoconfigure.openobserve.model.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@SpringBootApplication
public class OpenObserveBootTest {

    public static void main(String[] args) {
        SpringApplication.run(OpenObserveBootTest.class, args);
    }

    @Service
    public static class TestEventListener implements InitializingBean {
        @Resource
        private OpenTelemetry openTelemetry;
        @Resource
        private OpenObserveTemplate openObserveTemplate;

        /**
         *
         */
        @Override
        public void afterPropertiesSet() {
            log.info("初始化完成");

            log.info("开始测试");
            OpenLoggerProvider openLoggerProvider = new OpenLoggerProvider(null, openTelemetry);
            LogPolling logPolling = new LogPolling(openObserveTemplate);
            QueryBean queryBean = QueryBean.defaultQuery( "com.jbm.cluster.logs.mapper.GatewayLogsMapper.selectLogs");
            queryBean.getQuery().setSize(-1);
            logPolling.look(queryBean, (result)->{
                for (Map<String, Object> row : result.getHits()) {
                    Console.log(row);
                }
            });
            while (true) {
                openLoggerProvider.info("测试" + IdUtil.fastUUID());
                ThreadUtil.sleep(1000);
            }
        }


    }


}
