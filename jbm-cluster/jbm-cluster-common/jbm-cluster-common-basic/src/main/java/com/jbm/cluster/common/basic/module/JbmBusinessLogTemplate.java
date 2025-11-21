package com.jbm.cluster.common.basic.module;

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
import com.jbm.cluster.common.basic.module.log.BusinessLogContext;
import com.jbm.cluster.common.basic.module.log.BusinessLogSnapshot;
import com.jbm.cluster.common.basic.module.log.BusinessLogTraceUtils;
import lombok.extern.slf4j.Slf4j;
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

    @Value("${business.log.stream.enabled:true}")
    private boolean streamEnabled;

    @Value("${business.log.feign.enabled:true}")
    private boolean feignEnabled;

    private final ConcurrentMap<String, Object> traceLocks = new ConcurrentHashMap<>();

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
     * 生成阶段初始化日志字符串
     * 使用方式：log.info(JbmBusinessLogTemplate.stageInit("prepare,准备资源,1;process,处理数据,2;archive,归档输出,3"))
     * 
     * @param stages 阶段列表，格式：code,name,order（用分号分隔多个阶段）
     * @return 格式化的阶段初始化字符串，格式：[STAGE:INIT:stages]
     */
    public static String stageInit(String stages) {
        return StrUtil.format("[STAGE:INIT:{}]", stages);
    }

    /**
     * 生成阶段初始化日志字符串（便捷方法，单个阶段）
     * 使用方式：log.info(JbmBusinessLogTemplate.stageInit("prepare", "准备资源", 1))
     * 
     * @param stageCode 阶段代码
     * @param stageName 阶段名称
     * @param order 顺序
     * @return 格式化的阶段初始化字符串
     */
    public static String stageInit(String stageCode, String stageName, int order) {
        return stageInit(StrUtil.format("{},{},{}", stageCode, stageName, order));
    }

    /**
     * 生成阶段更新日志字符串
     * 使用方式：log.info(JbmBusinessLogTemplate.stageUpdate("prepare", "RUNNING", 20, "正在准备基础资源", 10))
     * 
     * @param stageCode 阶段代码
     * @param status 状态（WAITING/RUNNING/DONE/FAILED）
     * @param progress 进度（0-100）
     * @param message 消息
     * @param overallProgress 总体进度（0-100，可选）
     * @return 格式化的阶段更新字符串，格式：[STAGE:UPDATE:code,status,progress,message,overall]
     */
    public static String stageUpdate(String stageCode, String status, int progress, String message, Integer overallProgress) {
        String stageInfo = StrUtil.format("{},{},{},{}", stageCode, status, progress, message);
        if (overallProgress != null) {
            stageInfo += "," + overallProgress;
        }
        return StrUtil.format("[STAGE:UPDATE:{}]", stageInfo);
    }

    /**
     * 生成阶段更新日志字符串（简化版，不包含总体进度）
     * 使用方式：log.info(JbmBusinessLogTemplate.stageUpdate("prepare", "RUNNING", 20, "正在准备基础资源"))
     * 
     * @param stageCode 阶段代码
     * @param status 状态（WAITING/RUNNING/DONE/FAILED）
     * @param progress 进度（0-100）
     * @param message 消息
     * @return 格式化的阶段更新字符串
     */
    public static String stageUpdate(String stageCode, String status, int progress, String message) {
        return stageUpdate(stageCode, status, progress, message, null);
    }

    /**
     * 发布业务日志事件。
     */
    private void publishEvent(BusinessLogEventType eventType, String content) {
        publishEvent(eventType, content, null);
    }

    private void publishEvent(BusinessLogEventType eventType, String content, Integer overrideExpireDays) {
        BusinessLogSnapshot context = BusinessLogSnapshot.fromMdc(applicationName, overrideExpireDays,
                this::resolveCallerMethodName, DEFAULT_EXPIRE_DAYS);
        if (!context.valid()) {
            log.warn("MDC 未设置 businessType/businessId，使用默认上下文: {}", content);
        }

        String defaultBusinessType = resolveCallerMethodName();
        String defaultBusinessId = IdUtil.fastSimpleUUID();
        String finalBusinessType = StrUtil.blankToDefault(context.getBusinessType(), defaultBusinessType);
        String finalBusinessId = StrUtil.blankToDefault(context.getBusinessId(), defaultBusinessId);
        String finalLogId = resolveTraceIdentifier(context, finalBusinessId);

        BusinessLogEvent event = BusinessLogEvent.builder()
                .eventType(eventType)
                .logId(finalLogId)
                .businessType(finalBusinessType)
                .businessId(finalBusinessId)
                .content(context.decorate(content))
                .expireDays(context.getExpireDays())
                .source(context.getSource())
                .operator(context.getOperator())
                .operatorId(context.getOperatorId())
                .tenantId(context.getTenantId())
                .appId(context.getAppId())
                .timestamp(System.currentTimeMillis())
                .build();

        persistEvent(event);
        if (context.isFinished()) {
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
        String lockKey = BusinessLogTraceUtils.traceLockKey(event.getLogId());
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
                BusinessLogContext.clearByLockKey(lockKey);
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
                BusinessLogEvent event = JSON.parseObject(line, BusinessLogEvent.class);
                
                // 解析日志内容中的阶段信息
                if (event.getContent() != null && event.getContent().contains("[STAGE:")) {
                    List<BusinessLogEvent> stageEvents = parseStageEvents(event);
                    if (!stageEvents.isEmpty()) {
                        // 如果内容只包含阶段信息（去除空白后就是阶段信息），则不添加原始事件，只添加阶段事件
                        String trimmedContent = StrUtil.trim(event.getContent());
                        boolean isOnlyStage = trimmedContent.startsWith("[STAGE:") && trimmedContent.endsWith("]");
                        
                        if (isOnlyStage) {
                            // 只包含阶段信息，不添加原始事件，避免重复
                            events.addAll(stageEvents);
                        } else {
                            // 包含其他内容，添加原始事件和阶段事件
                            events.add(event);
                            events.addAll(stageEvents);
                        }
                    } else {
                        // 解析失败，仍然添加原始事件
                        events.add(event);
                    }
                } else {
                    // 不包含阶段信息，正常添加
                    events.add(event);
                }
            } catch (Exception ex) {
                log.error("解析本地业务日志失败: {}", line, ex);
            }
        }
        return events;
    }

    /**
     * 解析日志内容中的阶段信息，生成阶段事件
     */
    private List<BusinessLogEvent> parseStageEvents(BusinessLogEvent originalEvent) {
        List<BusinessLogEvent> stageEvents = new ArrayList<>();
        String content = originalEvent.getContent();
        
        if (StrUtil.isBlank(content)) {
            return stageEvents;
        }
        
        // 查找所有阶段信息：[STAGE:INIT:...] 或 [STAGE:UPDATE:...]
        int startIndex = 0;
        while (true) {
            int initIndex = content.indexOf("[STAGE:INIT:", startIndex);
            int updateIndex = content.indexOf("[STAGE:UPDATE:", startIndex);
            
            int nextIndex = -1;
            String stageType = null;
            String stageData = null;
            String prefix;
            
            if (initIndex >= 0 && (updateIndex < 0 || initIndex < updateIndex)) {
                nextIndex = initIndex;
                stageType = "INIT";
                prefix = "[STAGE:INIT:";
            } else if (updateIndex >= 0) {
                nextIndex = updateIndex;
                stageType = "UPDATE";
                prefix = "[STAGE:UPDATE:";
            } else {
                break;
            }
            
            // 找到匹配的右括号，从nextIndex开始查找
            int endIndex = content.indexOf("]", nextIndex + prefix.length());
            if (endIndex > nextIndex) {
                stageData = content.substring(nextIndex + prefix.length(), endIndex);
            }
            
            if (StrUtil.isBlank(stageData)) {
                startIndex = nextIndex + 1;
                continue;
            }
            
            // 创建阶段事件
            BusinessLogEvent stageEvent = BusinessLogEvent.builder()
                    .eventType("INIT".equals(stageType) ? BusinessLogEventType.STAGE_INIT : BusinessLogEventType.STAGE_UPDATE)
                    .logId(originalEvent.getLogId())
                    .businessType(originalEvent.getBusinessType())
                    .businessId(originalEvent.getBusinessId())
                    .content(stageData) // 阶段数据存储在content中
                    .expireDays(originalEvent.getExpireDays())
                    .source(originalEvent.getSource())
                    .operator(originalEvent.getOperator())
                    .operatorId(originalEvent.getOperatorId())
                    .tenantId(originalEvent.getTenantId())
                    .appId(originalEvent.getAppId())
                    .timestamp(originalEvent.getTimestamp())
                    .build();
            
            stageEvents.add(stageEvent);
            startIndex = endIndex + 1;
        }
        
        return stageEvents;
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
            } else if (event.getEventType() == BusinessLogEventType.STAGE_INIT 
                    || event.getEventType() == BusinessLogEventType.STAGE_UPDATE) {
                // 阶段事件通过extData传递，由日志服务解析处理
                // 这里先跳过，阶段事件会在日志服务端解析日志内容时处理
                success = true;
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
        String fileName = BusinessLogTraceUtils.traceLockKey(logId) + ".log";
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
        return StrUtil.blankToDefault(context.getTraceId(),
                StrUtil.blankToDefault(context.getLogId(),
                        StrUtil.blankToDefault(fallbackBusinessId, resolveCallerMethodName())));
    }

}
