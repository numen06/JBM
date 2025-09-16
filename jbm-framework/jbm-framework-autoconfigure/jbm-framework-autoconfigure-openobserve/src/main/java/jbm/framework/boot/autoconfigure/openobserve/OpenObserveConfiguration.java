package jbm.framework.boot.autoconfigure.openobserve;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

//@Configuration
@EnableConfigurationProperties(OpenObserveProperties.class)
@ConditionalOnProperty(prefix = "open-observe", name = "url")
public class OpenObserveConfiguration {

    @Bean
    public OpenObserveTemplate openObserveTemplate(OpenObserveProperties openObserveProperties) {
        return new OpenObserveTemplate(openObserveProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "open-observe", name = "otlp")
    public OpenTelemetry openTelemetry(OpenObserveProperties openObserveProperties) {
        // 创建 SdkLoggerProvider
        SdkLoggerProvider sdkLoggerProvider = SdkLoggerProvider.builder()
                .addLogRecordProcessor(BatchLogRecordProcessor.builder(
                                OtlpGrpcLogRecordExporter.builder()
                                        // OpenObserve OTLP 端点
                                        .setEndpoint(openObserveProperties.getUrl())
                                        .setTimeout(10, TimeUnit.SECONDS)
                                        .build())
                        .setScheduleDelay(500, TimeUnit.MILLISECONDS)
                        .build())
                .build();

        // 创建 OpenTelemetry 实例
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setLoggerProvider(sdkLoggerProvider)
                // 可选：用于 Trace
                .setTracerProvider(SdkTracerProvider.builder().build())
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();

        // JVM 关闭时优雅关闭
        Runtime.getRuntime().addShutdownHook(new Thread(sdkLoggerProvider::shutdown));

        return openTelemetry;
    }

}
