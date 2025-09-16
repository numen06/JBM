package jbm.framework.boot.autoconfigure.openobserve;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.PostConstruct;
import java.time.Duration;

public class OpenLoggerProvider implements InitializingBean {

    private final OpenObserveProperties openObserveProperties ;

    private final OpenTelemetry openTelemetry;

    public OpenLoggerProvider(OpenObserveProperties openObserveProperties, OpenTelemetry openTelemetry) {
        this.openObserveProperties = openObserveProperties;
        this.openTelemetry = openTelemetry;
    }


    /**
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
    }
}
