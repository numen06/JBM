package jbm.framework.boot.autoconfigure.ssl.event;

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

	private static final String PROPERTIES = "classpath:configs/ssl.properties";

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
			String keys = defaultProperties.getProperty("server.ssl.key-stores");
			String[] keyStores = keys.split(",");
			for (int i = 0; i < keyStores.length; i++) {
				Resource keyResource = resourceLoader.getResource(keyStores[i]);
				if (keyResource.exists()) {
					defaultProperties.put("server.ssl.key-store", keyResource.getURI());
					try {
						java.io.InputStream input = keyResource.getInputStream();
						input.close();
					} catch (Exception e) {
						continue;
					}
					break;
				}
			}
			PropertiesPropertySource freemarkPropertySource = new PropertiesPropertySource(resource.getFilename(), defaultProperties);
			event.getEnvironment().getPropertySources().addLast(freemarkPropertySource);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
