package com.jbm.test;

import cn.hutool.core.thread.ThreadUtil;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jbm.framework.boot.autoconfigure.openobserve.OpenLoggerProvider;
import jbm.framework.boot.autoconfigure.openobserve.OpenObserveTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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

        /**
         *
         */
        @Override
        public void afterPropertiesSet() {
            log.info("初始化完成");

            log.info("开始测试");
            OpenLoggerProvider openLoggerProvider = new OpenLoggerProvider(null,openTelemetry);
            openLoggerProvider.info("测试");
            Tracer tracer =  openTelemetry.getTracer("test-tracer");
            while ( true){

                // 创建并开始一个 Span
                Span span = tracer.spanBuilder("test-operation")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan();

                try (Scope scope = span.makeCurrent()) {
                    System.out.println("开始执行业务逻辑...");

                    // 模拟业务处理
                    Thread.sleep(100);

                    // 添加事件
                    span.addEvent("user.login");
                    span.setAttribute("user.id", "12345");
                    span.setAttribute("http.method", "GET");
                    span.setAttribute("http.url", "/api/login");

                    System.out.println("业务逻辑执行完成");
                } catch (Exception e) {
                    span.recordException(e);
                } finally {
                    span.end(); // ✅ 必须调用 end()
                }
                ThreadUtil.sleep(500);
            }
        }




    }




}
