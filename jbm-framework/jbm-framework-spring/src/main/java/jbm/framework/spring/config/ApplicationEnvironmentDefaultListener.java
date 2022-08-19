package jbm.framework.spring.config;

import cn.hutool.core.io.file.FileNameUtil;
import com.jbm.util.MapUtils;
import com.jbm.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public abstract class ApplicationEnvironmentDefaultListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static HashSet<String> configsMap = new HashSet<>();
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

    public abstract Map<String, Object> loadMapConfigs();

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        this.event = event;
        this.resourceLoader = event.getSpringApplication().getResourceLoader();
        try {
            this.resourceLoader = this.resourceLoader != null ? this.resourceLoader : new DefaultResourceLoader(getClassLoader());
            this.importProperties(event, resourceLoader);
            this.importProperties(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void importProperties(ApplicationEnvironmentPreparedEvent event, ResourceLoader resourceLoader) throws IOException {
        List<String> pps = this.loadPropertiesPath();
        if (CollectionUtils.isEmpty(pps)) {
            return;
        }
        for (String ss : pps) {
            String path = ss;
            if (!StringUtils.contains(path, "classpath:configs/")) {
                path = "classpath:configs/" + ss;
            }
//            Properties defaultProperties = new Properties();
//            Resource resource = resourceLoader.getResource(path);
//            defaultProperties.load(resource.getInputStream());
//            PropertiesPropertySource propertySource = new PropertiesPropertySource(resource.getFilename(), defaultProperties);
            if ("yml".equalsIgnoreCase(FileNameUtil.extName(path))) {
                this.loadYaml(path);
            } else {
                ResourcePropertySource resourcePropertySource = new ResourcePropertySource(path);
//            resourcePropertySource.getSource();
                this.addLast(resourcePropertySource);
            }
        }
    }

    private void loadYaml(String path) throws IOException {
        Resource resource = resourceLoader.getResource(path);
        YamlPropertySourceLoader yamlloader = new YamlPropertySourceLoader();
        List<PropertySource<?>> pss = yamlloader.load(getNameForResource(resource), resource);
        for (PropertySource propertySource : pss) {
            this.addLast(propertySource);
        }
    }

    private String getNameForResource(Resource resource) {
        String name = resource.getDescription();
        if (!org.springframework.util.StringUtils.hasText(name)) {
            name = resource.getClass().getSimpleName() + "@" + System.identityHashCode(resource);
        }
        return name;
    }


    private synchronized void addLast(PropertySource propertySource) {
        if (configsMap.contains(propertySource.getName()))
            return;
        event.getEnvironment().getPropertySources().addLast(propertySource);
        configsMap.add(propertySource.getName());
        logger.info("jbm load properties:{}", propertySource.getName());
    }

    protected void importProperties(ApplicationEnvironmentPreparedEvent event) {
        final Map<String, Object> map = this.loadMapConfigs();
        if (MapUtils.isEmpty(map))
            return;
        MapPropertySource propertySource = new MapPropertySource(this.getClass().getName(), map);
        this.addLast(propertySource);
    }

}
