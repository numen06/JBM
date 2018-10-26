package jbm.framework.boot.autoconfigure.tio.event;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

/**
 * 默认配置文件注入
 * 
 * @author wesley
 *
 */
public class ApplicationEnvironmentDefaultEvent implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		// RelaxedPropertyResolver propertyResolver =
		// RelaxedPropertyResolver.ignoringUnresolvableNestedPlaceholders(event.getEnvironment(),
		// "aio.");

	}

}
