package jbm.framework.boot.autoconfigure.mybatis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * 多数据源（baomidou dynamic-datasource）下 Hikari 全局默认的配套自动配置占位。
 * <p>
 * 实际将已解析的 {@code spring.datasource.hikari.*} 桥接到
 * {@code spring.datasource.dynamic.hikari.*} 的逻辑见
 * {@link jbm.framework.boot.autoconfigure.mybatis.event.DynamicDataSourceHikariPropertyBridgeListener}
 * （仅当目标键未配置时）；默认数值来自 {@code classpath:configs/mybatis-plus.properties} 中的
 * {@code spring.datasource.hikari.*}。
 *
 * @author wesley
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "com.baomidou.dynamic.datasource.DynamicRoutingDataSource")
public class DynamicDataSourceHikariDefaultConfiguration {
}
