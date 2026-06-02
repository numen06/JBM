package com.jbm.cluster.platform.gateway;

import com.jbm.cluster.common.mysql.mapper.GatewayRouteMapper;
import jbm.framework.boot.autoconfigure.eventbus.annotation.EnableClusterEventBus;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
/**
 * 网关启动程序
 *
 * @author wesley.zhang
 */
@EnableClusterEventBus(basePackages = "com.jbm.cluster")
@RemoteApplicationEventScan(basePackages = "com.jbm.cluster")
@SpringBootApplication(scanBasePackages = {"com.jbm.cluster.platform.gateway", "com.jbm.cluster.common.mysql"})
@EntityScan(basePackages = {"com.jbm.cluster.api.entitys"})
@MapperScan(basePackageClasses = GatewayRouteMapper.class)
public class JbmGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(JbmGatewayApplication.class, args);
    }
}
