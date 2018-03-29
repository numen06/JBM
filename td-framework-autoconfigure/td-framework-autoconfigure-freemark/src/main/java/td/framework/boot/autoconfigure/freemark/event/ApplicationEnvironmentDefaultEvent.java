package td.framework.boot.autoconfigure.freemark.event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

import com.td.util.StringUtils;

public class ApplicationEnvironmentDefaultEvent implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
	private static final Logger logger = LoggerFactory.getLogger(ApplicationEnvironmentDefaultEvent.class);
	ResourceLoader resourceLoader = null;

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
			Resource resource = resourceLoader.getResource("classpath:configs/freemark.properties");
			defaultProperties.load(resource.getInputStream());
			try {
				String targetClassPath = resourceLoader.getResource("/META-INF/html/").getURI().toString();
				String sourceClassPath = StringUtils.remove(targetClassPath, "/target/classes");
				String temp = defaultProperties.getProperty("spring.freemarker.template-loader-path");
				if (!"jar:".equals(StringUtils.left(sourceClassPath, 4))) {
					List<String> paths = new ArrayList<String>();
					paths.add(sourceClassPath);
					paths.add(temp);
					String templateLoaderPath = StringUtils.join(paths, ",");
					// System.err.println(templateLoaderPath);
					defaultProperties.put("spring.freemarker.template-loader-path", templateLoaderPath);
				}
				logger.info("freemark develop html path is {}", defaultProperties.getProperty("spring.freemarker.template-loader-path"));
			} catch (Exception e) {
				logger.warn("freemark read develop template-loader-path is error");
			}
			PropertiesPropertySource freemarkPropertySource = new PropertiesPropertySource(resource.getFilename(), defaultProperties);
			event.getEnvironment().getPropertySources().addLast(freemarkPropertySource);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
