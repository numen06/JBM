package com.jbm.cluster.common.basic.module;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.client.BusinessLogClient;
import com.jbm.cluster.api.form.log.AppendBusinessLogForm;
import com.jbm.cluster.api.form.log.CreateBusinessLogForm;
import com.jbm.cluster.api.model.log.BusinessLogEvent;
import com.jbm.cluster.api.model.log.BusinessLogEventType;
import com.jbm.cluster.core.constant.QueueConstants;
import com.jbm.framework.metadata.bean.ResultBody;
import lombok.*;
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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

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


    @Autowired(required = false)
    private StreamBridge streamBridge;

    @Autowired(required = false)
    private BusinessLogClient businessLogClient;

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    @Value("${business.log.storage.path:}")
    private String storageBasePath;

    @Value("${business.log.stream.enabled:false}")
    private boolean streamEnabled;

    @Value("${business.log.feign.enabled:false}")
    private boolean feignEnabled;

    private final ConcurrentMap<String, Object> traceLocks = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, BusinessLogContext> CONTEXT_CACHE = new ConcurrentHashMap<>();
    public static BusinessLogContext.BusinessLogContextBuilder log() {
        return BusinessLogContext.builder();
    }

    public static String logStart() {
        return BusinessLogContext.start();
    }

    public static String logStart(Consumer<BusinessLogContext.BusinessLogContextBuilder> customizer) {
        return BusinessLogContext.start(customizer);
    }

    public static void logEnd() {
        BusinessLogContext.end();
    }

    public static void logEnd(String logId) {
        BusinessLogContext.end(logId);
    }

    public static <T> T withLogContext(Supplier<T> supplier) {
        return BusinessLogContext.around(null, supplier);
    }

    public static <T> T withLogContext(Consumer<BusinessLogContext.BusinessLogContextBuilder> customizer, Supplier<T> supplier) {
        return BusinessLogContext.around(customizer, supplier);
    }

    public static void withLogContext(Runnable runnable) {
        BusinessLogContext.around(null, runnable);
    }

    public static void withLogContext(Consumer<BusinessLogContext.BusinessLogContextBuilder> customizer, Runnable runnable) {
        BusinessLogContext.around(customizer, runnable);
    }

    public static String currentLogId() {
        return BusinessLogContext.currentLogId();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        flushPendingTraceFiles("application-ready");
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
        BusinessLogSnapshot context = BusinessLogSnapshot.fromMdc(applicationName, overrideExpireDays, this::resolveCallerMethodName);
        if (!context.valid()) {
            log.warn("MDC 未设置 businessType/businessId，使用默认上下文: {}", content);
        }

        String defaultBusinessType = resolveCallerMethodName();
        String defaultBusinessId = IdUtil.fastSimpleUUID();
        String finalBusinessType = StrUtil.blankToDefault(context.businessType, defaultBusinessType);
        String finalBusinessId = StrUtil.blankToDefault(context.businessId, defaultBusinessId);
        String finalLogId = resolveTraceIdentifier(context, finalBusinessId);

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

        persistEvent(event);
        if (context.finished) {
            flushTraceLog(finalLogId, "mdc-finished");
        }
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

    private void persistEvent(BusinessLogEvent event) {
        if (event == null || StrUtil.isBlank(event.getLogId())) {
            return;
        }
        Path traceFile = resolveTraceFile(event.getLogId());
        String lockKey = traceLockKey(event.getLogId());
        Object lock = traceLocks.computeIfAbsent(lockKey, key -> new Object());
        String payload = JSON.toJSONString(event) + System.lineSeparator();
        synchronized (lock) {
            try {
                Files.createDirectories(traceFile.getParent());
                Files.write(traceFile, payload.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                log.error("写入本地业务日志文件失败: {}", traceFile, e);
            }
        }
    }

    private void flushTraceLog(String logId, String trigger) {
        if (StrUtil.isBlank(logId)) {
            return;
        }
        Path traceFile = resolveTraceFile(logId);
        flushTraceLogFile(traceFile, trigger);
    }

    private void flushTraceLogFile(Path traceFile, String trigger) {
        if (traceFile == null || !Files.exists(traceFile)) {
            return;
        }
        String lockKey = stripExtension(traceFile.getFileName().toString());
        Object lock = traceLocks.computeIfAbsent(lockKey, key -> new Object());
        Path uploading = null;
        synchronized (lock) {
            try {
                if (!Files.exists(traceFile)) {
                    return;
                }
                uploading = traceFile.resolveSibling(traceFile.getFileName() + ".uploading");
                Files.move(traceFile, uploading, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error("转移业务日志文件失败，trigger={}, file={}", trigger, traceFile, e);
                return;
            }
        }

        boolean success = false;
        try {
            List<BusinessLogEvent> events = readEvents(uploading);
            if (events.isEmpty()) {
                success = true;
            } else {
                success = trySendViaStream(events);
                if (!success) {
                    success = trySendViaFeign(events);
                }
            }
        } catch (Exception e) {
            log.error("重放业务日志文件失败: {}", uploading, e);
        } finally {
            handleUploadCompletion(traceFile, uploading, lockKey, success, trigger, lock);
        }
    }

    private void handleUploadCompletion(Path original, Path uploading, String lockKey,
                                        boolean success, String trigger, Object lock) {
        if (uploading == null) {
            return;
        }
        try {
            if (success) {
                Files.deleteIfExists(uploading);
                log.info("业务日志文件推送成功并删除: file={}, trigger={}", uploading, trigger);
                traceLocks.remove(lockKey, lock);
                clearCachedContextByLockKey(lockKey);
            } else {
                synchronized (lock) {
                    if (original != null) {
                        Files.move(uploading, original, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                log.warn("业务日志文件推送失败，已恢复为待推送: {}", original);
            }
        } catch (IOException e) {
            log.error("处理业务日志推送结果失败: {}", uploading, e);
        }
    }

    private List<BusinessLogEvent> readEvents(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<BusinessLogEvent> events = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (StrUtil.isBlank(line)) {
                continue;
            }
            try {
                events.add(JSON.parseObject(line, BusinessLogEvent.class));
            } catch (Exception ex) {
                log.error("解析本地业务日志失败: {}", line, ex);
            }
        }
        return events;
    }

    private boolean trySendViaStream(List<BusinessLogEvent> events) {
        if (!streamEnabled || streamBridge == null) {
            return false;
        }
        for (BusinessLogEvent event : events) {
            if (!sendBusinessLogEvent(event)) {
                return false;
            }
        }
        return true;
    }

    private boolean sendBusinessLogEvent(BusinessLogEvent event) {
        try {
            Message<BusinessLogEvent> message = MessageBuilder.withPayload(event).build();
            boolean sent = streamBridge.send(QueueConstants.BUSINESS_LOG_STREAM, message);
            if (!sent) {
                log.warn("业务日志事件发送失败: logId={}, type={}", event.getLogId(), event.getEventType());
            }
            return sent;
        } catch (Exception e) {
            log.error("发送业务日志事件异常: logId={}", event.getLogId(), e);
            return false;
        }
    }

    private boolean trySendViaFeign(List<BusinessLogEvent> events) {
        if (!feignEnabled || businessLogClient == null) {
            log.warn("Feign 客户端不可用，无法推送业务日志文件");
            return false;
        }
        for (BusinessLogEvent event : events) {
            boolean success;
            if (event.getEventType() == BusinessLogEventType.CREATE) {
                success = sendViaFeignCreate(event);
            } else if (event.getEventType() == BusinessLogEventType.APPEND) {
                success = sendViaFeignAppend(event);
            } else {
                log.debug("忽略不支持的事件类型: {}", event.getEventType());
                continue;
            }
            if (!success) {
                return false;
            }
        }
        return true;
    }

    private boolean sendViaFeignCreate(BusinessLogEvent event) {
        try {
            CreateBusinessLogForm form = new CreateBusinessLogForm();
            form.setBusinessType(event.getBusinessType());
            form.setBusinessId(event.getBusinessId());
            form.setContent(event.getContent());
            form.setExpireDays(event.getExpireDays());
            form.setSource(event.getSource());
            form.setTraceId(event.getLogId());
            ResultBody<?> response = businessLogClient.createLog(form);
            if (!isSuccess(response)) {
                log.warn("Feign 创建业务日志失败: logId={}, resp={}", event.getLogId(), response);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Feign 创建业务日志异常: logId={}", event.getLogId(), e);
            return false;
        }
    }

    private boolean sendViaFeignAppend(BusinessLogEvent event) {
        try {
            AppendBusinessLogForm form = new AppendBusinessLogForm();
            form.setLogId(StrUtil.blankToDefault(event.getLogId(), event.getBusinessId()));
            form.setContent(event.getContent());
            form.setAutoTimestamp(false);
            ResultBody<Boolean> response = businessLogClient.appendLog(form);
            boolean success = isSuccess(response) && Boolean.TRUE.equals(response.getResult());
            if (!success) {
                log.warn("Feign 追加业务日志失败: logId={}, resp={}", event.getLogId(), response);
            }
            return success;
        } catch (Exception e) {
            log.error("Feign 追加业务日志异常: logId={}", event.getLogId(), e);
            return false;
        }
    }

    private boolean isSuccess(ResultBody<?> response) {
        return response != null && Boolean.TRUE.equals(response.getSuccess());
    }

    private void flushPendingTraceFiles(String trigger) {
        Path dir = resolveStorageDir();
        if (!Files.exists(dir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.log")) {
            for (Path file : stream) {
                flushTraceLogFile(file, trigger);
            }
        } catch (IOException e) {
            log.error("扫描本地业务日志目录失败: {}", dir, e);
        }
    }

    private Path resolveTraceFile(String logId) {
        Path base = resolveStorageDir();
        String fileName = traceLockKey(logId) + ".log";
        return base.resolve(fileName);
    }

    private Path resolveStorageDir() {
        String baseDir = storageBasePath;
        if (StrUtil.isBlank(baseDir)) {
            String userDir = System.getProperty("user.dir", ".");
            baseDir = userDir + "/business-log/traces/" + applicationName;
        }
        return Paths.get(baseDir).toAbsolutePath();
    }

    private static String sanitizeFileName(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "unknown";
        }
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static BusinessLogContext getCachedContext(String logId) {
        if (StrUtil.isBlank(logId)) {
            return null;
        }
        return CONTEXT_CACHE.get(traceLockKey(logId));
    }

    private static void clearCachedContextByLockKey(String lockKey) {
        if (StrUtil.isBlank(lockKey)) {
            return;
        }
        CONTEXT_CACHE.remove(lockKey);
    }

    private static String traceLockKey(String logId) {
        return sanitizeFileName(StrUtil.blankToDefault(logId, "unknown"));
    }

    private String stripExtension(String name) {
        if (StrUtil.isBlank(name)) {
            return name;
        }
        if (name.endsWith(".log")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    private String resolveTraceIdentifier(BusinessLogSnapshot context, String fallbackBusinessId) {
        return StrUtil.blankToDefault(context.traceId,
                StrUtil.blankToDefault(context.logId,
                        StrUtil.blankToDefault(fallbackBusinessId, resolveCallerMethodName())));
    }











    /**
     * 基于 MDC 的上下文封装。
     */
    private static class BusinessLogSnapshot {
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
        private final boolean finished;

        private BusinessLogSnapshot(String businessType, String businessId, String source,
                                   Integer expireDays, String logId, String traceId, String operator,
                                   String operatorId, String tenantId, String appId,
                                   boolean autoTimestamp, boolean finished) {
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
            this.finished = finished;
        }

        static BusinessLogSnapshot fromMdc(String defaultSource, Integer overrideExpireDays,
                                           Supplier<String> callerSupplier) {
            Map<String, String> mdc = MDC.getCopyOfContextMap();
            String threadLogId = BusinessLogContext.currentLogId();

            String logId = firstNonBlankValue(threadLogId, firstNonBlank(mdc, "businessLogId"),
                    firstNonBlank(mdc, "traceId"));
            BusinessLogContext cached = getCachedContext(logId);

            String fallbackType = defaultSource;
            String fallbackId = callerSupplier != null ? callerSupplier.get() : defaultSource;

            String businessType = StrUtil.blankToDefault(cached != null ? cached.getBusinessType() : null, fallbackType);
            String businessId = StrUtil.blankToDefault(cached != null ? cached.getBusinessId() : null,
                    StrUtil.blankToDefault(logId, fallbackId));

            String source = StrUtil.blankToDefault(cached != null ? cached.getSource() : null, defaultSource);

            Integer expireDays = cached != null && cached.getExpireDays() != null
                    ? cached.getExpireDays()
                    : (overrideExpireDays != null ? overrideExpireDays : DEFAULT_EXPIRE_DAYS);

            String operator = cached != null ? cached.getOperator() : null;
            String operatorId = cached != null ? cached.getOperatorId() : null;
            String tenantId = cached != null ? cached.getTenantId() : null;
            String appId = cached != null ? cached.getAppId() : source;

            boolean autoTimestamp = cached == null || cached.getAutoTimestamp() == null
                    || Boolean.TRUE.equals(cached.getAutoTimestamp());

            boolean finished = cached != null && Boolean.TRUE.equals(cached.getFinished());

            if (StrUtil.isBlank(logId)) {
                logId = StrUtil.blankToDefault(threadLogId, IdUtil.fastSimpleUUID());
            }
            String traceId = StrUtil.blankToDefault(cached != null ? cached.getTraceId() : null,
                    firstNonBlank(mdc, "traceId"));
            if (StrUtil.isBlank(traceId)) {
                traceId = logId;
            }

            return new BusinessLogSnapshot(businessType, businessId, source, expireDays,
                    logId, traceId, operator, operatorId, tenantId, appId, autoTimestamp, finished);
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

        private static String firstNonBlankValue(String... values) {
            if (values == null) {
                return null;
            }
            for (String value : values) {
                if (StrUtil.isNotBlank(value)) {
                    return value;
                }
            }
            return null;
        }
    }

    /**
     * 简化业务方写入 MDC 的方式，避免到处硬编码 key。
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class BusinessLogContext {

        private static final ThreadLocal<String> CURRENT_LOG_ID = new ThreadLocal<>();
        private String logId;
        private String traceId;
        private String businessType;
        private String businessId;
        private String source;
        private Integer expireDays;
        private String operator;
        private String operatorId;
        private String tenantId;
        private String appId;
        private Boolean autoTimestamp;
        private Boolean finished;

        public static String start() {
            return start(builder -> {});
        }

        public static String start(Consumer<BusinessLogContextBuilder> customizer) {
            BusinessLogContextBuilder builder = BusinessLogContext.builder();
            if (customizer != null) {
                customizer.accept(builder);
            }
            return start(builder.build());
        }

        public static String start(BusinessLogContext payload) {
            BusinessLogContext context = prepareContext(payload);
            writeMdc(context);
            return context.getLogId();
        }

        public static void end() {
            end(null);
        }

        public static void end(String logId) {
            String effectiveLogId = StrUtil.blankToDefault(logId,
                    StrUtil.blankToDefault(CURRENT_LOG_ID.get(), MDC.get("businessLogId")));
            if (StrUtil.isBlank(effectiveLogId)) {
                return;
            }
            CONTEXT_CACHE.computeIfPresent(traceLockKey(effectiveLogId), (key, ctx) -> {
                ctx.setFinished(true);
                return ctx;
            });
            MDC.put("businessLogFinished", "true");
            MDC.remove("businessLogId");
            MDC.remove("traceId");
            CURRENT_LOG_ID.remove();
        }

        public static <T> T around(Supplier<T> supplier) {
            return around(null, supplier);
        }

        public static <T> T around(Consumer<BusinessLogContextBuilder> customizer, Supplier<T> supplier) {
            String logId = start(customizer);
            try {
                return supplier.get();
            } finally {
                end(logId);
            }
        }

        public static void around(Runnable runnable) {
            around(null, runnable);
        }

        public static void around(Consumer<BusinessLogContextBuilder> customizer, Runnable runnable) {
            String logId = start(customizer);
            try {
                runnable.run();
            } finally {
                end(logId);
            }
        }

        private static BusinessLogContext prepareContext(BusinessLogContext payload) {
            BusinessLogContext incoming = payload != null ? payload : new BusinessLogContext();
            String logId = StrUtil.blankToDefault(incoming.getLogId(), incoming.getTraceId());
            if (StrUtil.isBlank(logId)) {
                logId = IdUtil.fastSimpleUUID();
            }
            if (StrUtil.isBlank(incoming.getTraceId())) {
                incoming.setTraceId(logId);
            }
            incoming.setLogId(logId);
            String cacheKey = traceLockKey(logId);
            BusinessLogContext merged = CONTEXT_CACHE.compute(cacheKey, (key, existing) -> mergeContext(existing, incoming));
            if (merged == null) {
                merged = copyContext(incoming);
                CONTEXT_CACHE.put(cacheKey, merged);
            }
            CURRENT_LOG_ID.set(logId);
            return merged;
        }

        private static BusinessLogContext mergeContext(BusinessLogContext base, BusinessLogContext incoming) {
            if (base == null) {
                return copyContext(incoming);
            }
            if (StrUtil.isNotBlank(incoming.getBusinessType())) {
                base.setBusinessType(incoming.getBusinessType());
            }
            if (StrUtil.isNotBlank(incoming.getBusinessId())) {
                base.setBusinessId(incoming.getBusinessId());
            }
            if (StrUtil.isNotBlank(incoming.getSource())) {
                base.setSource(incoming.getSource());
            }
            if (incoming.getExpireDays() != null) {
                base.setExpireDays(incoming.getExpireDays());
            }
            if (StrUtil.isNotBlank(incoming.getOperator())) {
                base.setOperator(incoming.getOperator());
            }
            if (StrUtil.isNotBlank(incoming.getOperatorId())) {
                base.setOperatorId(incoming.getOperatorId());
            }
            if (StrUtil.isNotBlank(incoming.getTenantId())) {
                base.setTenantId(incoming.getTenantId());
            }
            if (StrUtil.isNotBlank(incoming.getAppId())) {
                base.setAppId(incoming.getAppId());
            }
            if (incoming.getAutoTimestamp() != null) {
                base.setAutoTimestamp(incoming.getAutoTimestamp());
            }
            if (incoming.getFinished() != null) {
                base.setFinished(incoming.getFinished());
            }
            if (StrUtil.isNotBlank(incoming.getTraceId())) {
                base.setTraceId(incoming.getTraceId());
            }
            base.setLogId(incoming.getLogId());
            return base;
        }

        private static BusinessLogContext copyContext(BusinessLogContext source) {
            if (source == null) {
                return new BusinessLogContext();
            }
            return BusinessLogContext.builder()
                    .logId(source.getLogId())
                    .traceId(source.getTraceId())
                    .businessType(source.getBusinessType())
                    .businessId(source.getBusinessId())
                    .source(source.getSource())
                    .expireDays(source.getExpireDays())
                    .operator(source.getOperator())
                    .operatorId(source.getOperatorId())
                    .tenantId(source.getTenantId())
                    .appId(source.getAppId())
                    .autoTimestamp(source.getAutoTimestamp())
                    .finished(source.getFinished())
                    .build();
        }

        private static void writeMdc(BusinessLogContext ctx) {
            MDC.put("businessLogId", ctx.getLogId());
            if (StrUtil.isNotBlank(ctx.getTraceId())) {
                MDC.put("traceId", ctx.getTraceId());
            }
            MDC.remove("businessLogFinished");
        }

        private static String currentLogId() {
            String value = CURRENT_LOG_ID.get();
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
            return MDC.get("businessLogId");
        }
    }
}

