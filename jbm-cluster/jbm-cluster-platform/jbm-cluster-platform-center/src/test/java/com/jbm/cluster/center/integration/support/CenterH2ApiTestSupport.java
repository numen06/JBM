package com.jbm.cluster.center.integration.support;

import com.jbm.cluster.center.JbmCenterApplication;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.doNothing;

/**
 * Center H2 集成测试基类（无 MockMvc，直接注入 Bean 验证业务逻辑）。
 */
@SpringBootTest(
        classes = JbmCenterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.autoconfigure.exclude=com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration,com.alibaba.cloud.nacos.NacosConfigAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,jbm.framework.boot.autoconfigure.redis.RedisAutoConfiguration",
                "jbm.cluster.api-register=false",
                "management.endpoints.enabled-by-default=false"
        }
)
@ActiveProfiles("h2")
@Import(CenterH2TestConfiguration.class)
public abstract class CenterH2ApiTestSupport {

    @MockBean
    protected JbmClusterTemplate jbmClusterTemplate;

    @MockBean
    protected RedisService redisService;

    @BeforeEach
    void baseSetUp() {
        doNothing().when(jbmClusterTemplate).refreshGateway();
    }
}
