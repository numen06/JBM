package jbm.framework.boot.autoconfigure.mybatis.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 当 classpath 存在 baomidou {@code DynamicRoutingDataSource} 时，将
 * {@code spring.datasource.hikari.*} 中已解析的值桥接到
 * {@code spring.datasource.dynamic.hikari.*}（仅目标键未显式配置时）。
 * <p>
 * 解决多数据源场景下仅配置 {@code spring.datasource.hikari.maximum-pool-size} 等
 * 对 Spring Boot 单数据源生效、对 dynamic-datasource 不生效的问题。
 *
 * @author wesley
 */
@Slf4j
public class DynamicDataSourceHikariPropertyBridgeListener
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private static final String SOURCE_NAME = "jbm.dynamic-datasource.hikari-bridge";

    private static final String STANDARD_HIKARI_PREFIX = "spring.datasource.hikari.";
    private static final String DYNAMIC_HIKARI_PREFIX = "spring.datasource.dynamic.hikari.";

    /**
     * 与 {@code mybatis-plus.properties} 中 Hikari 项对齐；不桥接 pool-name，避免多数据源同名冲突。
     */
    private static final List<String> BRIDGE_KEYS = Arrays.asList(
            "maximum-pool-size",
            "minimum-idle",
            "connection-timeout",
            "max-lifetime",
            "idle-timeout",
            "keepalive-time",
            "leak-detection-threshold"
    );

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ClassLoader classLoader = event.getSpringApplication().getClassLoader();
        if (!ClassUtils.isPresent("com.baomidou.dynamic.datasource.DynamicRoutingDataSource", classLoader)) {
            return;
        }

        ConfigurableEnvironment env = event.getEnvironment();
        Map<String, Object> bridge = new LinkedHashMap<>();
        for (String key : BRIDGE_KEYS) {
            String target = DYNAMIC_HIKARI_PREFIX + key;
            if (env.getProperty(target) != null) {
                continue;
            }
            String source = STANDARD_HIKARI_PREFIX + key;
            String value = env.getProperty(source);
            if (value != null) {
                bridge.put(target, value);
            }
        }

        if (bridge.isEmpty()) {
            return;
        }

        if (env.getPropertySources().contains(SOURCE_NAME)) {
            env.getPropertySources().remove(SOURCE_NAME);
        }
        env.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, bridge));
        log.info("JBM: 已为 dynamic-datasource 桥接 {} 项 Hikari 配置（{} -> {}）",
                bridge.size(), STANDARD_HIKARI_PREFIX, DYNAMIC_HIKARI_PREFIX);
    }
}
