package jbm.framework.spring.config;

import com.jbm.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public abstract class ApplicationEnvironmentDefaultListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected ResourceLoader resourceLoader = null;

    protected ApplicationEnvironmentPreparedEvent event;


    public ClassLoader getClassLoader() {
        if (this.resourceLoader != null) {
            return this.resourceLoader.getClassLoader();
        }
        return ClassUtils.getDefaultClassLoader();
    }

    public abstract List<String> loadPropertiesPath();

    public abstract Properties initProperties();


    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        logger.info("start load prop");
        this.event = event;
        this.resourceLoader = event.getSpringApplication().getResourceLoader();
        try {
            ResourceLoader resourceLoader = this.resourceLoader != null ? this.resourceLoader : new DefaultResourceLoader(getClassLoader());
            this.importProperties(event, resourceLoader);
            this.importProperties(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("end load prop");
    }

    protected void importProperties(ApplicationEnvironmentPreparedEvent event, ResourceLoader resourceLoader) throws IOException {
        List<String> pps = this.loadPropertiesPath();
        if (CollectionUtils.isEmpty(pps))
            return;
        for (String ss : pps) {
            String path = ss;
            if (!StringUtils.contains("classpath:configs/")) {
                path = "classpath:configs/" + ss;
            }
            Properties defaultProperties = new Properties();
            Resource resource = resourceLoader.getResource(path);
            defaultProperties.load(resource.getInputStream());
            PropertiesPropertySource propertySource = new PropertiesPropertySource(resource.getFilename(), defaultProperties);
            event.getEnvironment().getPropertySources().addLast(propertySource);
        }
    }

    protected void importProperties(ApplicationEnvironmentPreparedEvent event) {
        if (this.initProperties() == null)
            return;
        PropertiesPropertySource propertySource = new PropertiesPropertySource(this.getClass().getName(), this.initProperties());
        event.getEnvironment().getPropertySources().addLast(propertySource);
    }

}
