package jbm.framework.boot.autoconfigure.mysql.event;

import java.io.IOException;
import java.util.Properties;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

/**
 * 默认配置文件注入
 * 
 * @author wesley
 *
 */
public class ApplicationEnvironmentDefaultEvent implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	private static final String PROPERTIES = "classpath:configs/jpa.properties";

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
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
