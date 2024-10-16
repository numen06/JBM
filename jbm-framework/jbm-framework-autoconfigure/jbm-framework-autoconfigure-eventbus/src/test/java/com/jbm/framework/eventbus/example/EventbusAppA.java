package com.jbm.framework.eventbus.example;

import cn.hutool.extra.spring.EnableSpringUtil;
import com.jbm.framework.eventbus.example.event.BusConfiguration;
import com.jbm.framework.eventbus.example.event.TestRemoteEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author wesley.zhang
 * @create 2021/7/14 7:38 下午
 * @email numen06@qq.com
 * @description 事件bus
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableSpringUtil
//@RemoteApplicationEventScan(basePackageClasses = {TestRemoteEvent.class})
@RemoteApplicationEventScan(basePackageClasses = {BusConfiguration.class})
public class EventbusAppA {

//    @Resource
//    public EventPublisher eventPublisher;

    public static void main(String[] args) {
        System.setProperty("send", "true");
        SpringApplication.run(EventbusAppA.class, args);
    }

    @EventListener
    public void onApplicationEvent(TestRemoteEvent event) {
        try {
            log.info("应用A接受：{}:{}", event.getTitle(), event.getMsg());
        } catch (Exception e) {
            log.error("失败", e);
        }
    }
}