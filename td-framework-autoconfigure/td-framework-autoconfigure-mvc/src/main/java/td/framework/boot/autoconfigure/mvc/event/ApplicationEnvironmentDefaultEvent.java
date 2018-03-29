package td.framework.boot.autoconfigure.mvc.event;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

public class ApplicationEnvironmentDefaultEvent implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
	private static final Logger logger = LoggerFactory.getLogger(ApplicationEnvironmentDefaultEvent.class);

	private static final String PROPERTIES = "classpath:configs/mvc.properties";

	private ResourceLoader resourceLoader = null;

	public ResourceLoader getResourceLoader() {
		return resourceLoader;
	}

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
//			String[] paths = StringUtils.split(event.getEnvironment().getProperty("spring.resources.static-locations"), ",");
//			for (int i = 0; i < paths.length; i++) {
//				String path = paths[i];
//				paths[i] = resourceLoader.getResource(path).getFilename();
//			}
			logger.info("resoures develop path is {}", event.getEnvironment().getProperty("spring.resources.static-locations"));
		} catch (IOException e) {
			logger.error("读取配置文件{}错误", PROPERTIES, e);
		}

	}

}
