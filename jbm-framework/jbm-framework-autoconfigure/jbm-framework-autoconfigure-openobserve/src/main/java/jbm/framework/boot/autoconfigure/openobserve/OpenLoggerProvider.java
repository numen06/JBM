package jbm.framework.boot.autoconfigure.openobserve;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import org.springframework.beans.factory.InitializingBean;

import java.time.Instant;

public class OpenLoggerProvider implements InitializingBean {

    private final OpenObserveProperties openObserveProperties ;

    private final OpenTelemetry openTelemetry;

    private final Logger logger;

    public OpenLoggerProvider(OpenObserveProperties openObserveProperties, OpenTelemetry openTelemetry) {
        this.openObserveProperties = openObserveProperties;
        this.openTelemetry = openTelemetry;
        this.logger = this.openTelemetry.getLogsBridge().get("test");
    }

    public void info(String message) {
        logger.logRecordBuilder()
                .setBody(message)
                .setSeverity(Severity.INFO)
                .setTimestamp(Instant.now())
                .emit();
    }

    public void error(String message) {
        logger.logRecordBuilder()
                .setBody(message)
                .setSeverity(Severity.ERROR)
                .setTimestamp(Instant.now())
                .emit();
    }

    public void warn(String message) {
        logger.logRecordBuilder()
                .setBody(message)
                .setSeverity(Severity.WARN)
                .setTimestamp(Instant.now())
                .emit();
    }

    /**
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
    }
}
