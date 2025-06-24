package com.jbm.cluster.logs;


import com.jbm.cluster.logs.tdengine.mapper.GatewayLogsMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 日志收集基础服务
 *
 * @author wesley.zhang
 */
@EnableCaching
@SpringBootApplication
@MapperScan(basePackageClasses = GatewayLogsMapper.class)
public class JbmLogsApplication {
//    public final static String TD_DATASOURCE = "tdengine";

    public static void main(String[] args) {
        SpringApplication.run(JbmLogsApplication.class, args);
    }

}

