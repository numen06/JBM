package com.jbm.micro.sb.test;

import javax.annotation.PostConstruct;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import com.jbm.micro.sb.test.service.SpringBootService;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@EnableAutoConfiguration // 启用自动配置
@ComponentScan // 组件扫描
public class SpringBootMvcTest extends AbstractScheduledService {

    private static final int port = RandomUtil.randomInt(1000, 8000);

    @PostConstruct
    private void init() {
        this.startAsync();
    }

    public static void main(String[] args) {
        System.setProperty("server.port", StrUtil.toString(port));
        // 启动Spring Boot项目的唯一入口
        SpringApplication.run(SpringBootMvcTest.class, args);
    }

    @Override
    protected void runOneIteration() throws Exception {
        String url = StrUtil.format("http://localhost:{}/test", port);
        log.info("访问地址：{}", url);
        String body = HttpUtil.get(url);
        log.info("返回结果：{}", body);
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedDelaySchedule(0, 1, TimeUnit.SECONDS);
    }
}
