package jbm.framework.boot.autoconfigure.openobserve;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 校验 {@link OpenObserveConfiguration} 在具备 {@code open-observe.url} 时会注册 {@link OpenObserveTemplate}。
 */
class OpenObserveConfigurationBeanTest {

    @Test
    void registersOpenObserveTemplateWhenUrlSet() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("testOpenObserve", Collections.singletonMap("open-observe.url", "http://127.0.0.1:5080")));
            ctx.register(OpenObserveConfiguration.class);
            ctx.refresh();
            assertNotNull(ctx.getBean(OpenObserveTemplate.class));
        }
    }
}
