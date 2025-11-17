package jbm.framework.boot.autoconfigure.emqx;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.Properties;


/**
 * 默认配置文件注入
 *
 * @author wesley
 */
@Slf4j
public class ApplicationEnvironmentDefaultEvent implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String PROPERTIES = "classpath:configs/emqx.properties";

    @Getter
    private ResourceLoader resourceLoader = null;

    public ClassLoader getClassLoader() {
        if (this.resourceLoader != null) {
            return this.resourceLoader.getClassLoader();
        }
        return ClassUtils.getDefaultClassLoader();
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Properties defaultProperties = new Properties();
        this.resourceLoader = event.getSpringApplication().getResourceLoader();
        try {
            ResourceLoader resourceLoader = this.getResourceLoader() != null ? this.getResourceLoader() : new DefaultResourceLoader(getClassLoader());
            Resource resource = resourceLoader.getResource(PROPERTIES);
            defaultProperties.load(resource.getInputStream());
            PropertiesPropertySource freemarkPropertySource = new PropertiesPropertySource(resource.getFilename(), defaultProperties);
            event.getEnvironment().getPropertySources().addLast(freemarkPropertySource);
        } catch (IOException e) {
            log.error("配置[{}]预注入失败", PROPERTIES, e);
        }

    }

}
