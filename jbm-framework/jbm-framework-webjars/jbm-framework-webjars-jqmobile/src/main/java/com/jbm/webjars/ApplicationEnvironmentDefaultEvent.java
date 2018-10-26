package com.jbm.webjars;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import com.jbm.util.StringUtils;


public class ApplicationEnvironmentDefaultEvent implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
	private static final Log logger = LogFactory.getLog(ApplicationEnvironmentDefaultEvent.class);
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
				if (resourceLoader.getResource(temp).exists() && !"jar:".equals(StringUtils.left(sourceClassPath, 4))) {
					List<String> paths = new ArrayList<String>();
					paths.add(sourceClassPath);
					paths.add(temp);
					String templateLoaderPath = StringUtils.join(paths, ",");
					// System.err.println(templateLoaderPath);
					defaultProperties.put("spring.freemarker.template-loader-path", templateLoaderPath);
					logger.info("freemark develop html path is " + sourceClassPath);
				}
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
