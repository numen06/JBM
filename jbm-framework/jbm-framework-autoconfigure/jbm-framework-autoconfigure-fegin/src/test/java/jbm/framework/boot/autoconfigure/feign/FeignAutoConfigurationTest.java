package jbm.framework.boot.autoconfigure.feign;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class FeignAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class))
            .withBean(FastJsonHttpMessageConverter.class, FastJsonHttpMessageConverter::new);

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void forwardsBusinessHeadersWithoutCopyingHopByHopHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-a");
        request.addHeader("Transfer-Encoding", "chunked");
        request.addHeader("Connection", "keep-alive");
        request.addHeader("Host", "gateway.example.com");
        request.addHeader("Cookie", "wms={%22token%22:%22Bearer%20test%22}");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestTemplate template = new RequestTemplate();

        new FeignRequestInterceptor().apply(template);

        assertThat(template.headers().get("X-Tenant-Id")).containsExactly("tenant-a");
        assertThat(template.headers()).doesNotContainKeys("Transfer-Encoding", "Connection", "Host", "Cookie");
    }

    @Test
    void registersHeaderForwardingInterceptorWhenNoDedicatedInterceptorExists() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RequestInterceptor.class);
            assertThat(context).hasBean("feignRequestInterceptor");
        });
    }

    @Test
    void keepsGenericInterceptorAlongsideUnrelatedInterceptor() {
        RequestInterceptor dedicatedInterceptor = template -> template.header("X-Test", "dedicated");

        contextRunner
                .withBean("dedicatedRequestInterceptor", RequestInterceptor.class, () -> dedicatedInterceptor)
                .run(context -> {
                    assertThat(context).hasBean("feignRequestInterceptor");
                    assertThat(context.getBeansOfType(RequestInterceptor.class)).hasSize(2);
                    assertThat(context.getBean("dedicatedRequestInterceptor")).isSameAs(dedicatedInterceptor);
                });
    }

    @Test
    void loadsClusterInterceptorBeforeGenericFallback() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        FeignAutoConfiguration.class,
                        com.jbm.cluster.common.feign.FeignAutoConfiguration.class))
                .withBean(FastJsonHttpMessageConverter.class, FastJsonHttpMessageConverter::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(RequestInterceptor.class);
                    assertThat(context).hasBean("clusterRequestInterceptor");
                    assertThat(context).doesNotHaveBean("feignRequestInterceptor");
                });
    }
}
