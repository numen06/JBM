package jbm.framework.boot.autoconfigure.feign;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import feign.RequestInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FeignAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class))
            .withBean(FastJsonHttpMessageConverter.class, FastJsonHttpMessageConverter::new);

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
