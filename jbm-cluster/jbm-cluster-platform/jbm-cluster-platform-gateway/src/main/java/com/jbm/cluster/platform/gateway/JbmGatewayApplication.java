package com.jbm.cluster.platform.gateway;

import com.jbm.cluster.common.mysql.mapper.GatewayRouteMapper;
import jbm.framework.boot.autoconfigure.eventbus.annotation.EnableClusterEventBus;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
/**
 * 网关启动程序
 *
 * @author wesley.zhang
 */
@EnableClusterEventBus(basePackages = "com.jbm.cluster")
@RemoteApplicationEventScan(basePackages = "com.jbm.cluster")
@SpringBootApplication
@ComponentScan(
        basePackages = {"com.jbm.cluster.platform.gateway", "com.jbm.cluster.common.mysql"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = {
                        "com\\.jbm\\.cluster\\.common\\.mysql\\.init\\..*",
                        "com\\.jbm\\.cluster\\.common\\.mysql\\.service\\.impl\\.(CustomForms.*|Extend.*)"
                }))
@EntityScan(basePackages = {"com.jbm.cluster.api.entitys"})
@MapperScan(basePackageClasses = GatewayRouteMapper.class)
public class JbmGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(JbmGatewayApplication.class, args);
    }
}
