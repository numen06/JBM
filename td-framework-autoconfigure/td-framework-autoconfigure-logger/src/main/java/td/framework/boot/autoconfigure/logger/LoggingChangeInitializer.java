package td.framework.boot.autoconfigure.logger;

import org.springframework.boot.logging.LoggingApplicationListener;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.logback.RemoteLogbackLoggingSystem;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.Ordered;

import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;

public class LoggingChangeInitializer extends LoggingApplicationListener {
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 19;

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (StringUtils.isBlank(System.getProperty("file.encoding"))) {
			System.setProperty("file.encoding", "UTF-8");
		}
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY, RemoteLogbackLoggingSystem.class.getName());
	}

	@Override
	public int getOrder() {
		return DEFAULT_ORDER;
	}

}
