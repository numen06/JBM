package com.jbm.cluster.common.basic.module;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.model.log.BusinessLogEvent;
import com.jbm.cluster.api.model.log.BusinessLogEventType;
import com.jbm.cluster.core.constant.QueueConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationListener;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 业务日志模板（MDC 优先 + 全异步）
 *
 * <p>使用方式：在业务代码中为每次请求设置 MDC 上下文（businessType、businessId、traceId 等），
 * 然后调用 startLog/appendLog 即可异步采集日志，最终由日志服务统一处理。</p>
 *
 * <p>保留的能力：</p>
 * <ul>
 *     <li>startLog：基于当前 MDC 创建一条日志（发送 CREATE 事件）</li>
 *     <li>appendLog：基于当前 MDC 追加日志内容（发送 APPEND 事件）</li>
 *     <li>appendError：快速记录异常堆栈</li>
 * </ul>
 *
 * <p>其它同步/文件/实时/URL 等功能全部移除，保证模板极简、专注于 MDC 场景。</p>
 *
 * @author wesley
 */
@Slf4j
public class JbmBusinessLogTemplate  implements ApplicationListener<ApplicationReadyEvent> {

    private static final int DEFAULT_EXPIRE_DAYS = 30;

    private static final String MDC_BUSINESS_TYPE = "businessType";
    private static final String MDC_BUSINESS_ID = "businessId";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SOURCE = "source";
    private static final String MDC_OPERATOR = "operator";
    private static final String MDC_OPERATOR_ID = "operatorId";
    private static final String MDC_TENANT_ID = "tenantId";
    private static final String MDC_APP_ID = "appId";
    private static final String MDC_EXPIRE_DAYS = "businessLogExpireDays";
    private static final String MDC_LOG_ID = "businessLogId";
    private static final String MDC_AUTO_TIMESTAMP = "businessLogAutoTimestamp";
    private static final String MDC_FINISHED = "businessLogFinished";

    @Autowired
    private StreamBridge streamBridge;

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    @Value("${business.log.buffer.enabled:false}")
    private boolean bufferEnabled;

    @Value("${business.log.buffer.collect-first:false}")
    private boolean bufferCollectFirst;

    @Value("${business.log.buffer.path:}")
    private String bufferFilePath;

    @Value("${business.log.archive.enabled:true}")
    private boolean archiveEnabled;

    @Value("${business.log.archive.base-path:}")
    private String archiveBasePath;

    private final Object bufferLock = new Object();
    private final ConcurrentMap<Path, Object> archiveLocks = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        flushBufferFile("application-ready");
    }
    /**
     * 基于当前 MDC 创建一条业务日志。
     *
     * @param content 初始化内容
     */
    public void startLog(String content) {
        publishEvent(BusinessLogEventType.CREATE, content);
    }

    /**
     * 基于当前 MDC 创建一条业务日志，允许临时覆盖过期天数。
     *
     * @param content 初始化内容
     * @param overrideExpireDays 临时过期天数（null 时仍读取 MDC 或默认值）
     */
    public void startLog(String content, Integer overrideExpireDays) {
        publishEvent(BusinessLogEventType.CREATE, content, overrideExpireDays);
    }

    /**
     * 基于当前 MDC 追加日志内容。
     *
     * @param content 追加内容
     */
    public void appendLog(String content) {
        publishEvent(BusinessLogEventType.APPEND, content);
    }

    /**
     * 支持模板格式化的追加方式，方便快速拼装上下文。
     *
     * @param template 模板
     * @param args 参数
     */
    public void appendLogf(String template, Object... args) {
        appendLog(StrUtil.format(template, args));
    }

    /**
     * 记录异常信息（自动包含堆栈）。
     *
     * @param message 描述
     * @param throwable 异常
     */
    public void appendError(String message, Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        builder.append("[ERROR] ").append(message);
        if (throwable != null) {
            builder.append("\n异常: ").append(throwable.getClass().getName());
            builder.append("\n消息: ").append(throwable.getMessage());
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                builder.append("\n堆栈: ");
                int limit = Math.min(stackTrace.length, 8);
                for (int i = 0; i < limit; i++) {
                    builder.append("\n  at ").append(stackTrace[i]);
                }
            }
        }
        publishEvent(BusinessLogEventType.APPEND, builder.toString());
    }

    /**
     * 发布业务日志事件。
     */
    private void publishEvent(BusinessLogEventType eventType, String content) {
        publishEvent(eventType, content, null);
    }

    private void publishEvent(BusinessLogEventType eventType, String content, Integer overrideExpireDays) {
        BusinessLogContext context = BusinessLogContext.fromMdc(applicationName, overrideExpireDays);
        if (!context.valid()) {
            log.warn("MDC 未设置 businessType/businessId，使用默认上下文: {}", content);
        }

        String defaultBusinessId = resolveCallerMethodName();
        String finalBusinessType = StrUtil.blankToDefault(context.businessType, applicationName);
        String finalBusinessId = StrUtil.blankToDefault(context.businessId, defaultBusinessId);
        String finalLogId = StrUtil.blankToDefault(context.traceId,
                StrUtil.blankToDefault(context.logId, finalBusinessId));

        BusinessLogEvent event = BusinessLogEvent.builder()
                .eventType(eventType)
                .logId(finalLogId)
                .businessType(finalBusinessType)
                .businessId(finalBusinessId)
                .content(context.decorate(content))
                .expireDays(context.expireDays)
                .source(context.source)
                .operator(context.operator)
                .operatorId(context.operatorId)
                .tenantId(context.tenantId)
                .appId(context.appId)
                .timestamp(System.currentTimeMillis())
                .build();

        archiveEvent(event);
        if (bufferEnabled && bufferCollectFirst) {
            bufferEvent(event, null);
            flushBufferFile("collect-first");
            return;
        }
        sendBusinessLogEvent(event);
    }

    private String resolveCallerMethodName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String templateClassName = getClass().getName();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.equals(templateClassName) || className.startsWith("java.lang.Thread")) {
                continue;
            }
            return className + "." + element.getMethodName();
        }
        return applicationName + "#unknown";
    }

    /**
     * 发送业务日志事件到消息队列。
     */
    private void sendBusinessLogEvent(BusinessLogEvent event) {
        try {
            if (streamBridge == null) {
                log.warn("StreamBridge 未注入，业务日志将写入缓冲文件: {}", JSON.toJSONString(event));
                bufferEvent(event, new IllegalStateException("StreamBridge is null"));
                return;
            }

            Message<BusinessLogEvent> message = MessageBuilder.withPayload(event).build();
            boolean sent = streamBridge.send(QueueConstants.BUSINESS_LOG_STREAM, message);
            if (!sent) {
                log.warn("业务日志事件发送失败，准备写入缓冲文件: {}", JSON.toJSONString(event));
                bufferEvent(event, new IllegalStateException("StreamBridge send returned false"));
            } else {
                log.debug("业务日志事件已发送: type={}, businessType={}, businessId={}",
                        event.getEventType(), event.getBusinessType(), event.getBusinessId());
            }
        } catch (Exception e) {
            log.error("发送业务日志事件异常，将写入缓冲文件", e);
            bufferEvent(event, e);
        }
    }

    private void bufferEvent(BusinessLogEvent event, Exception reason) {
        if (!bufferEnabled) {
            return;
        }
        try {
            Path path = resolveBufferPath();
            String payload = JSON.toJSONString(event);
            String record;
            if (reason == null) {
                record = StrUtil.format("{} | {}\n", DateUtil.now(), payload);
            } else {
                record = StrUtil.format("{} | reason={} | {}\n",
                        DateUtil.now(),
                        StrUtil.blankToDefault(reason.getMessage(), reason.getClass().getSimpleName()),
                        payload);
            }
            synchronized (bufferLock) {
                Files.createDirectories(path.getParent());
                Files.write(path, record.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            log.info("业务日志事件已写入本地缓冲: {}", path);
        } catch (IOException ioe) {
            log.error("写入业务日志缓冲文件失败", ioe);
        }
    }

    private void archiveEvent(BusinessLogEvent event) {
        if (!archiveEnabled || event == null) {
            return;
        }
        try {
            Path file = resolveArchiveFile(event);
            Files.createDirectories(file.getParent());
            String line = formatArchiveLine(event);
            Object lock = archiveLocks.computeIfAbsent(file, key -> new Object());
            synchronized (lock) {
                Files.write(file, line.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException ioe) {
            log.warn("写入业务日志归档文件失败: businessType={}, businessId={}",
                    event.getBusinessType(), event.getBusinessId(), ioe);
        }
    }

    private String formatArchiveLine(BusinessLogEvent event) {
        String ts = DateUtil.formatDateTime(DateUtil.date(event.getTimestamp()));
        String source = StrUtil.blankToDefault(event.getSource(), applicationName);
        String operator = StrUtil.blankToDefault(event.getOperator(), "-");
        String header = StrUtil.format("[{}][{}][{}][operator:{}]",
                ts, event.getEventType(), source, operator);
        return header + " " + StrUtil.nullToEmpty(event.getContent()) + System.lineSeparator();
    }

    private Path resolveArchiveFile(BusinessLogEvent event) {
        String baseDir = archiveBasePath;
        if (StrUtil.isBlank(baseDir)) {
            String userHome = System.getProperty("user.home", ".");
            baseDir = userHome + "/.business-log/archive/" + applicationName;
        }
        String businessType = sanitizeFileName(StrUtil.blankToDefault(event.getBusinessType(), "default"));
        String identifier = sanitizeFileName(
                StrUtil.blankToDefault(event.getBusinessId(),
                        StrUtil.blankToDefault(event.getLogId(), DateUtil.now())));
        return Paths.get(baseDir, businessType, identifier + ".log");
    }

    private String sanitizeFileName(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "unknown";
        }
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Path resolveBufferPath() {
        String actualPath = bufferFilePath;
        if (StrUtil.isBlank(actualPath)) {
            String userHome = System.getProperty("user.home", ".");
            actualPath = userHome + "/.business-log/buffer/" + applicationName + "-buffer.log";
        }
        return Paths.get(actualPath);
    }

    private void flushBufferFile(String trigger) {
        if (!bufferEnabled) {
            return;
        }
        Path path = resolveBufferPath();
        if (!Files.exists(path)) {
            return;
        }
        String[] lines;
        synchronized (bufferLock) {
            try {
                lines = Files.readAllLines(path, StandardCharsets.UTF_8).toArray(new String[0]);
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.error("读取业务日志缓冲文件失败，trigger={}", trigger, e);
                return;
            }
        }
        log.info("准备重放 {} 条业务日志缓冲记录，trigger={}", lines.length, trigger);
        for (String line : lines) {
            BusinessLogEvent event = parseBufferedEvent(line);
            if (event == null) {
                continue;
            }
            sendBusinessLogEvent(event);
        }
    }

    private BusinessLogEvent parseBufferedEvent(String line) {
        if (StrUtil.isBlank(line)) {
            return null;
        }
        int jsonStart = line.indexOf('{');
        if (jsonStart < 0) {
            return null;
        }
        String json = line.substring(jsonStart);
        try {
            return JSON.parseObject(json, BusinessLogEvent.class);
        } catch (Exception ex) {
            log.error("解析业务日志缓冲记录失败: {}", line, ex);
            return null;
        }
    }



    /**
     * 基于 MDC 的上下文封装。
     */
    private static class BusinessLogContext {
        private final String businessType;
        private final String businessId;
        private final String source;
        private final Integer expireDays;
        private final String logId;
        private final String traceId;
        private final String operator;
        private final String operatorId;
        private final String tenantId;
        private final String appId;
        private final boolean autoTimestamp;

        private BusinessLogContext(String businessType, String businessId, String source,
                                   Integer expireDays, String logId, String traceId, String operator,
                                   String operatorId, String tenantId, String appId,
                                   boolean autoTimestamp) {
            this.businessType = businessType;
            this.businessId = businessId;
            this.source = source;
            this.expireDays = expireDays;
            this.logId = logId;
            this.traceId = traceId;
            this.operator = operator;
            this.operatorId = operatorId;
            this.tenantId = tenantId;
            this.appId = appId;
            this.autoTimestamp = autoTimestamp;
        }

        static BusinessLogContext fromMdc(String defaultSource, Integer overrideExpireDays) {
            Map<String, String> mdc = MDC.getCopyOfContextMap();
            String businessType = firstNonBlank(mdc, MDC_BUSINESS_TYPE);
            String businessId = firstNonBlank(mdc, MDC_BUSINESS_ID);
            String traceId = firstNonBlank(mdc, MDC_TRACE_ID);

            String source = firstNonBlank(mdc, MDC_SOURCE);
            if (StrUtil.isBlank(source)) {
                source = defaultSource;
            }

            Integer expireDays = overrideExpireDays != null ?
                    overrideExpireDays : parseExpireDays(firstNonBlank(mdc, MDC_EXPIRE_DAYS));
            String logId = firstNonBlank(mdc, MDC_LOG_ID, traceId);
            String operator = firstNonBlank(mdc, MDC_OPERATOR);
            String operatorId = firstNonBlank(mdc, MDC_OPERATOR_ID);
            String tenantId = firstNonBlank(mdc, MDC_TENANT_ID);
            String appId = firstNonBlank(mdc, MDC_APP_ID, source);
            boolean autoTimestamp = !"false".equalsIgnoreCase(firstNonBlank(mdc, MDC_AUTO_TIMESTAMP));

            return new BusinessLogContext(businessType, businessId, source, expireDays,
                    logId, traceId, operator, operatorId, tenantId, appId, autoTimestamp);
        }

        boolean valid() {
            return StrUtil.isNotBlank(businessType) && StrUtil.isNotBlank(businessId);
        }

        String decorate(String content) {
            if (StrUtil.isBlank(content)) {
                return content;
            }
            if (!autoTimestamp) {
                return content;
            }
            DateTime now = DateUtil.date();
            return StrUtil.format("[{}] {}", DateUtil.formatDateTime(now), content);
        }

        private static String firstNonBlank(Map<String, String> ctx, String... keys) {
            if (ctx == null || keys == null) {
                return null;
            }
            for (String key : keys) {
                if (key == null) {
                    continue;
                }
                String value = ctx.get(key);
                if (StrUtil.isNotBlank(value)) {
                    return value;
                }
            }
            return null;
        }

        private static Integer parseExpireDays(String raw) {
            if (StrUtil.isBlank(raw)) {
                return DEFAULT_EXPIRE_DAYS;
            }
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                return DEFAULT_EXPIRE_DAYS;
            }
        }
    }

    /**
     * 简化业务方写入 MDC 的方式，避免到处硬编码 key。
     */
    public static final class BusinessLogMdc {

        private BusinessLogMdc() {
        }

        public static void bindBusiness(String businessType, String businessId) {
            put(MDC_BUSINESS_TYPE, businessType);
            put(MDC_BUSINESS_ID, businessId);
        }

        public static void bindSource(String source) {
            put(MDC_SOURCE, source);
        }

        public static void bindOperator(String operatorId, String operatorName) {
            put(MDC_OPERATOR_ID, operatorId);
            put(MDC_OPERATOR, operatorName);
        }

        public static void bindTenant(String tenantId) {
            put(MDC_TENANT_ID, tenantId);
        }

        public static void bindApp(String appId) {
            put(MDC_APP_ID, appId);
        }

        public static void overrideExpireDays(Integer expireDays) {
            if (expireDays == null) {
                MDC.remove(MDC_EXPIRE_DAYS);
            } else {
                MDC.put(MDC_EXPIRE_DAYS, String.valueOf(expireDays));
            }
        }

        public static void enableAutoTimestamp() {
            put(MDC_AUTO_TIMESTAMP, "true");
        }

        public static void disableAutoTimestamp() {
            put(MDC_AUTO_TIMESTAMP, "false");
        }

        public static void setLogId(String logId) {
            put(MDC_LOG_ID, logId);
        }

        public static void markFinished() {
            MDC.put(MDC_FINISHED, "true");
        }

        public static void clearBusinessKeys() {
            MDC.remove(MDC_BUSINESS_TYPE);
            MDC.remove(MDC_BUSINESS_ID);
            MDC.remove(MDC_SOURCE);
            MDC.remove(MDC_OPERATOR);
            MDC.remove(MDC_OPERATOR_ID);
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_APP_ID);
            MDC.remove(MDC_EXPIRE_DAYS);
            MDC.remove(MDC_LOG_ID);
            MDC.remove(MDC_AUTO_TIMESTAMP);
            MDC.remove(MDC_FINISHED);
        }

        private static void put(String key, String value) {
            if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(value)) {
                MDC.put(key, value);
            }
        }
    }
}

