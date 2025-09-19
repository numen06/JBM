package jbm.framework.boot.autoconfigure.openobserve;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

//@Configuration
@EnableConfigurationProperties(OpenObserveProperties.class)
@ConditionalOnProperty(prefix = "open-observe", name = "url")
public class OpenObserveConfiguration {

    @Bean
    public OpenObserveTemplate openObserveTemplate(OpenObserveProperties openObserveProperties) {
        return new OpenObserveTemplate(openObserveProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "open-observe", name = "grpc")
    public OpenTelemetry openTelemetry(OpenObserveProperties openObserveProperties) {
        // 获取OpenTelemetry Tracer
        Resource resource = Resource.getDefault();

//        OtlpGrpcSpanExporter logSpan = OtlpGrpcSpanExporter.builder()
//                .setEndpoint(openObserveProperties.getOtlp())
//                .addHeader("Authorization", "Basic YWRtaW5AZXhhbXBsZS5jb206MURmMUI4QWU2M0JvU0Q1WA==")
//                .addHeader("organization", "default")
//                .addHeader("X-OBSERVE-DATASET", "my-stream")
////                .addHeader("stream", "test")
//                .build();
//
//        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
//                .setResource(resource)
//                .addSpanProcessor(
//                        BatchSpanProcessor.builder(logSpan)
//                                .setScheduleDelay(Duration.ofSeconds(1))
//                                .build()
//                )
//                .build();

        LogRecordExporter logExporter = OtlpGrpcLogRecordExporter.builder()
                .setEndpoint(openObserveProperties.getGrpc())
                .addHeader("Authorization", "Basic YWRtaW5AZXhhbXBsZS5jb206MURmMUI4QWU2M0JvU0Q1WA==")
                .addHeader("organization", "default")
                .addHeader("x-observe-dataset", "my-stream")
                .addHeader("stream","my-stream")
                .build();


        SdkLoggerProvider sdkLoggerProvider = SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(
                        BatchLogRecordProcessor.builder(logExporter)
                                .setScheduleDelay(Duration.ofSeconds(1)) // 每秒发送一次
                                .build()
                )
                .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
//                .setTracerProvider(sdkTracerProvider)
                .setLoggerProvider(sdkLoggerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();

        // JVM 关闭时优雅关闭
        Runtime.getRuntime().addShutdownHook(new Thread(sdkLoggerProvider::shutdown));

        return openTelemetry;
    }

}
