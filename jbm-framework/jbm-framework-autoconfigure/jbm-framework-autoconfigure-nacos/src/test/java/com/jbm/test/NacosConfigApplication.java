package com.jbm.test;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.alibaba.nacos.spring.context.annotation.config.NacosPropertySource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@Slf4j
@SpringBootApplication
@NacosPropertySource(dataId = "redis", autoRefreshed = true)
public class NacosConfigApplication {


    @NacosValue(value = "${spring.redis.host:false}", autoRefreshed = true)
    private String useLocalCache;

    public static void main(String[] args) {
        SpringApplication.run(NacosConfigApplication.class, args);
    }

    @PostConstruct
    public void ppt() {
        log.info(useLocalCache);
    }

}