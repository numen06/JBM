package jbm.framework.boot.autoconfigure.logger;

import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.Ordered;

public class LoggingChangeInitializer extends LoggingApplicationListener {
    public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 19;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (StringUtils.isBlank(System.getProperty("file.encoding"))) {
            System.setProperty("file.encoding", "UTF-8");
        }
//		System.setProperty(LoggingSystem.SYSTEM_PROPERTY, RemoteLogbackLoggingSystem.class.getName());
    }

    @Override
    public int getOrder() {
        return DEFAULT_ORDER;
    }

}
