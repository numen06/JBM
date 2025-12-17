package com.jbm.cluster.logs.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson.JSONObject;
import com.jbm.cluster.api.entitys.log.BusinessLog;
import com.jbm.cluster.api.entitys.log.BusinessLogStageItem;
import com.jbm.cluster.api.entitys.log.BusinessLogStageSnapshot;
import com.jbm.cluster.api.form.log.AppendBusinessLogForm;
import com.jbm.cluster.api.form.log.BusinessLogForm;
import com.jbm.cluster.api.form.log.BusinessLogStageUpdateForm;
import com.jbm.cluster.api.form.log.CreateBusinessLogForm;
import com.jbm.cluster.api.form.log.InitBusinessLogStageForm;
import com.jbm.cluster.logs.service.BusinessLogService;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jbm.framework.boot.autoconfigure.openobserve.OpenObserveTemplate;
import jbm.framework.boot.autoconfigure.openobserve.model.QueryBean;
import jbm.framework.boot.autoconfigure.openobserve.model.QueryResult;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 业务日志服务实现类
 * 基于OpenObserve实现业务日志的存储和查询
 * 
 * @author wesley
 */
@Service
@Slf4j
public class BusinessLogServiceImpl implements BusinessLogService {
    
    @Resource
    private OpenObserveTemplate openObserveTemplate;
    
    @Autowired(required = false)
    private RedisService redisService;
    
    @Autowired(required = false)
    private OpenTelemetry openTelemetry;
    
    /**
     * 获取Tracer实例（用于创建追踪Span）
     */
    private Tracer getTracer() {
        if (openTelemetry != null) {
            return openTelemetry.getTracer("business-log-service", "1.0.0");
        }
        return null;
    }
    
    /**
     * 从当前上下文获取或生成traceId
     * 如果当前有活跃的Span，使用其traceId；否则生成新的traceId
     * 
     * @param providedTraceId 外部提供的traceId（可选）
     * @return traceId
     */
    private String getOrCreateTraceId(String providedTraceId) {
        // 如果外部提供了traceId，优先使用
        if (StrUtil.isNotEmpty(providedTraceId)) {
            return providedTraceId;
        }
        
        // 尝试从当前Span上下文获取traceId
        if (openTelemetry != null) {
            Span currentSpan = Span.current();
            if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
                String traceId = currentSpan.getSpanContext().getTraceId();
                if (StrUtil.isNotEmpty(traceId)) {
                    return traceId;
                }
            }
        }
        
        // 如果没有活跃的Span，生成新的traceId（实际应该创建新的Span）
        return IdUtil.fastSimpleUUID();
    }
    
    /**
     * 创建业务日志追踪Span
     * 将业务日志关联到追踪系统中，便于在OpenObserve中查看完整的调用链
     * 
     * @param logId 业务日志ID
     * @param module 业务模块
     * @param operation 操作类型
     * @return Span实例，用于后续添加事件和属性
     */
    private Span createBusinessLogSpan(String logId, String module, String operation) {
        Tracer tracer = getTracer();
        if (tracer == null) {
            return null;
        }
        
        return tracer.spanBuilder("business_log." + operation)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute(AttributeKey.stringKey("log.id"), logId)
                .setAttribute(AttributeKey.stringKey("log.module"), module)
                .setAttribute(AttributeKey.stringKey("log.operation"), operation)
                .startSpan();
    }
    
    /**
     * 在Span中添加业务日志事件
     * 
     * @param span Span实例
     * @param content 日志内容
     * @param lineNumber 行号
     */
    private void addLogEventToSpan(Span span, String content, Integer lineNumber) {
        if (span == null) {
            return;
        }
        
        Attributes attributes = Attributes.builder()
                .put(AttributeKey.stringKey("log.content"), content)
                .put(AttributeKey.longKey("log.line_number"), lineNumber != null ? lineNumber.longValue() : 0L)
                .build();
        
        span.addEvent("log.append", attributes);
    }
    
    /**
     * OpenObserve流名称前缀
     * 根据过期时间使用不同的流，充分利用OpenObserve的自动过期功能
     */
    private static final String STREAM_NAME_PREFIX = "business_log";
    
    /**
     * 临时URL token密钥（实际应该从配置读取）
     */
    private static final String TOKEN_SECRET = "jbm-business-log-secret-key-2025";
    
    /**
     * Token缓存前缀
     */
    private static final String TOKEN_CACHE_PREFIX = "business_log_token:";
    
    /**
     * 已初始化的流缓存（避免重复创建）
     */
    private final Set<String> initializedStreams = Collections.synchronizedSet(new HashSet<>());
    
    /**
     * logId到流名的本地缓存（线程安全，避免重复查询）
     * 使用ConcurrentHashMap存储，key为logId，value为streamName
     */
    private final Map<String, String> logIdStreamCache = new ConcurrentHashMap<>();
    /**
     * 业务日志阶段缓存
     */
    private final Map<String, BusinessLogStageTracker> stageTrackers = new ConcurrentHashMap<>();
    private static final String STATEMENT_SELECT_BY_LOG_ID_STREAM = "com.jbm.cluster.logs.mapper.BusinessLogMapper.queryLogByStream";
    private static final String STATEMENT_SELECT_LOGS_BY_STREAM = "com.jbm.cluster.logs.mapper.BusinessLogMapper.queryLogsByStream";
    private static final String[] ALL_STREAM_NAMES = {"business_log_30d", "business_log_7d", "business_log_90d", "business_log"};
    
    /**
     * 根据过期天数获取流类型标识
     * 用于生成有规则的logId
     * 
     * @param expireDays 过期天数
     * @return 流类型标识（如：7d, 30d, 90d, 365d）
     */
    private String getStreamType(Integer expireDays) {
        if (expireDays == null || expireDays <= 0) {
            expireDays = 30;
        }
        
        // 根据过期时间返回流类型标识
        if (expireDays <= 7) {
            return "7d";
        } else if (expireDays <= 30) {
            return "30d";
        } else if (expireDays <= 90) {
            return "90d";
        } else {
            return expireDays + "d";
        }
    }
    
    /**
     * 根据过期天数获取流名称
     * 将相同过期时间的日志放到同一个流中，利用OpenObserve的流级别TTL自动过期
     * 
     * @param expireDays 过期天数
     * @return 流名称
     */
    private String getStreamName(Integer expireDays) {
        if (expireDays == null || expireDays <= 0) {
            // 永不过期或默认30天
            expireDays = 30;
        }
        
        // 根据过期时间分组到不同的流：
        // - 7天以内：business_log_7d
        // - 30天以内：business_log_30d
        // - 90天以内：business_log_90d
        // - 其他：business_log_{days}d
        String streamName;
        if (expireDays <= 7) {
            streamName = STREAM_NAME_PREFIX + "_7d";
        } else if (expireDays <= 30) {
            streamName = STREAM_NAME_PREFIX + "_30d";
        } else if (expireDays <= 90) {
            streamName = STREAM_NAME_PREFIX + "_90d";
        } else {
            streamName = STREAM_NAME_PREFIX + "_" + expireDays + "d";
        }
        
        // 确保流已创建并配置了保留策略
        ensureStreamInitialized(streamName, expireDays);
        
        return streamName;
    }
    
    /**
     * 根据流类型标识获取流名称
     * 
     * @param streamType 流类型标识（如：7d, 30d, 90d）
     * @return 流名称
     */
    private String getStreamNameByType(String streamType) {
        if (StrUtil.isEmpty(streamType)) {
            return STREAM_NAME_PREFIX + "_30d";
        }
        return STREAM_NAME_PREFIX + "_" + streamType;
    }
    
    /**
     * 生成有规则的logId
     * 格式：{streamType}_{timestamp}_{random}
     * 例如：30d_1735689600_a1b2c3d4e5f6
     * 
     * 优势：
     * 1. 从logId中可以直接解析出流类型，避免遍历所有流查询
     * 2. 包含时间戳，便于查询时快速定位时间范围
     * 3. 保证唯一性
     * 
     * @param expireDays 过期天数
     * @return 有规则的logId
     */
    private String generateRuleBasedLogId(Integer expireDays) {
        // 获取流类型标识
        String streamType = getStreamType(expireDays);
        
        // 获取当前时间戳（秒级）
        long timestamp = System.currentTimeMillis() / 1000;
        
        // 生成随机字符串（8位，保证唯一性）
        String random = IdUtil.fastSimpleUUID().substring(0, 8);
        
        // 组合：{streamType}_{timestamp}_{random}
        return streamType + "_" + timestamp + "_" + random;
    }
    
    /**
     * 从logId中解析流类型标识
     * 支持两种格式：
     * 1. 新格式：{streamType}_{timestamp}_{random}，例如：30d_1735689600_a1b2c3d4
     * 2. 旧格式：纯UUID（向后兼容）
     * 
     * @param logId 日志ID
     * @return 流类型标识，如果无法解析则返回null
     */
    private String parseStreamTypeFromLogId(String logId) {
        if (StrUtil.isEmpty(logId)) {
            return null;
        }
        
        // 检查是否为新格式：{streamType}_{timestamp}_{random}
        int firstUnderscore = logId.indexOf('_');
        if (firstUnderscore > 0 && firstUnderscore < logId.length() - 1) {
            String streamType = logId.substring(0, firstUnderscore);
            // 验证流类型格式（应该是数字+d，如：7d, 30d, 90d, 365d）
            if (streamType.matches("\\d+d")) {
                return streamType;
            }
        }
        
        // 旧格式或无法解析，返回null
        return null;
    }
    
    /**
     * 从logId中解析时间戳（秒级）
     * 仅支持新格式的logId
     * 
     * @param logId 日志ID
     * @return 时间戳（秒级），如果无法解析则返回null
     */
    private Long parseTimestampFromLogId(String logId) {
        if (StrUtil.isEmpty(logId)) {
            return null;
        }
        
        // 检查是否为新格式：{streamType}_{timestamp}_{random}
        int firstUnderscore = logId.indexOf('_');
        int secondUnderscore = logId.indexOf('_', firstUnderscore + 1);
        
        if (firstUnderscore > 0 && secondUnderscore > firstUnderscore && secondUnderscore < logId.length() - 1) {
            try {
                String timestampStr = logId.substring(firstUnderscore + 1, secondUnderscore);
                return Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                // 时间戳格式错误
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * 确保流已初始化并配置了保留策略
     * 
     * @param streamName 流名称
     * @param retentionDays 保留天数
     */
    private void ensureStreamInitialized(String streamName, Integer retentionDays) {
        if (!initializedStreams.contains(streamName)) {
            synchronized (this) {
                if (!initializedStreams.contains(streamName)) {
                    openObserveTemplate.ensureStreamWithRetention(streamName, retentionDays);
                    initializedStreams.add(streamName);
                    log.debug("已初始化流 {}，保留策略: {} 天", streamName, retentionDays);
                }
            }
        }
    }
    
    /**
     * 根据logId确定它所在的流
     * 优先从logId中解析流类型（新格式），如果无法解析则查询数据库（向后兼容旧格式）
     * 
     * @param logId 日志ID
     * @return 流名称，如果找不到则返回null
     */
    private String determineStreamByLogId(String logId) {
        if (StrUtil.isEmpty(logId)) {
            return null;
        }
        
        // 先检查本地缓存
        String cachedStream = logIdStreamCache.get(logId);
        if (StrUtil.isNotEmpty(cachedStream)) {
            return cachedStream;
        }
        
        // 优先尝试从logId中解析流类型（新格式：{streamType}_{timestamp}_{random}）
        String streamType = parseStreamTypeFromLogId(logId);
        if (StrUtil.isNotEmpty(streamType)) {
            String streamName = getStreamNameByType(streamType);
            // 缓存到本地
            logIdStreamCache.put(logId, streamName);
            log.info("✓ 从logId {} 解析出流类型: {}，流名: {}", logId, streamType, streamName);
            return streamName;
        } else {
            log.debug("logId {} 无法解析流类型（可能是旧格式UUID）", logId);
        }
        
        // 如果无法解析（旧格式UUID），则从数据库查询（向后兼容）
        log.debug("logId {} 为旧格式，从数据库查询确定流", logId);
        return determineStreamByLogIdFromDb(logId);
    }
    
    /**
     * 从数据库查询确定logId所在的流（用于向后兼容旧格式的logId）
     * 
     * @param logId 日志ID
     * @return 流名称，如果找不到则返回null
     */
    private String determineStreamByLogIdFromDb(String logId) {
        // 尝试从logId中解析时间戳，用于优化查询时间范围
        Long timestamp = parseTimestampFromLogId(logId);
        Date beginTime;
        Date endTime;
        
        if (timestamp != null) {
            // 如果能解析出时间戳，使用时间戳前后各1天作为查询范围
            beginTime = new Date((timestamp - 86400) * 1000L); // 前1天
            endTime = new Date((timestamp + 86400) * 1000L); // 后1天
        } else {
            // 无法解析时间戳，使用默认范围（过去7天到未来1小时）
            Date now = new Date();
            beginTime = DateUtil.offsetDay(now, -7);
            endTime = DateUtil.offsetHour(now, 1);
        }
        
        // 从最常用的流开始查询（优先查询business_log_30d，因为大多数日志都是30天过期）
        String[] streamNames = {"business_log_30d", "business_log_7d", "business_log_90d", "business_log"};
        
        for (String streamName : streamNames) {
            try {
                // 查询该流中是否有这个logId的记录
                String sql = String.format("SELECT * FROM %s WHERE log_id = '%s' LIMIT 1", streamName, logId);
                
                QueryBean queryBean = new QueryBean();
                queryBean.getQuery().setSql(sql);
                queryBean.getQuery().setFrom(0);
                queryBean.getQuery().setSize(1);
                queryBean.getQuery().setStartTime(beginTime.getTime() * 1000);
                queryBean.getQuery().setEndTime(endTime.getTime() * 1000);
                
                QueryResult queryResult = openObserveTemplate.selectLogs(queryBean);
                List<Map<String, Object>> hits = queryResult.getHits();
                
                if (hits != null && !hits.isEmpty()) {
                    // 找到了记录，从记录中获取expireDays并计算流名
                    JSONObject jsonObject = new JSONObject(hits.get(0));
                    BusinessLog businessLog = jsonObject.toJavaObject(BusinessLog.class);
                    
                    Integer expireDays = businessLog.getExpireDays();
                    if (expireDays != null && expireDays > 0) {
                        String calculatedStream = getStreamName(expireDays);
                        // 缓存到本地
                        logIdStreamCache.put(logId, calculatedStream);
                        log.debug("确定logId {} 所在的流: {} (从流 {} 查询到，expireDays: {})", 
                                logId, calculatedStream, streamName, expireDays);
                        return calculatedStream;
                    } else {
                        // 如果没有expireDays，使用当前查询到的流
                        logIdStreamCache.put(logId, streamName);
                        log.debug("确定logId {} 所在的流: {} (从记录中获取)", logId, streamName);
                        return streamName;
                    }
                }
            } catch (Exception e) {
                log.debug("查询流 {} 确定logId {} 失败: {}", streamName, logId, e.getMessage());
            }
        }
        
        // 如果所有流都查不到，返回null
        log.warn("无法确定logId {} 所在的流", logId);
        return null;
    }
    
    // 其余业务逻辑...
    
    @Override
    public String createLog(CreateBusinessLogForm form) {
        // 确定过期天数
        Integer expireDays = form.getExpireDays() != null && form.getExpireDays() > 0 ? form.getExpireDays() : 30;
        
        // 生成有规则的业务日志ID（格式：{streamType}_{timestamp}_{random}）
        // 例如：30d_1735689600_a1b2c3d4
        String logId = generateRuleBasedLogId(expireDays);
        
        // 获取或创建traceId，关联到追踪系统
        String traceId = getOrCreateTraceId(form.getTraceId());
        
        // 创建业务日志追踪Span
        Span span = createBusinessLogSpan(logId, form.getModule(), form.getOperation());
        Scope scope = span != null ? span.makeCurrent() : null;
        
        try {
            Date now = new Date();
            Date expireDate = DateUtil.offsetDay(now, expireDays);
            
            // 在Span中添加属性
            if (span != null) {
                span.setAttribute(AttributeKey.stringKey("log.user_id"), StrUtil.nullToDefault(form.getUserId(), ""));
                span.setAttribute(AttributeKey.stringKey("log.username"), StrUtil.nullToDefault(form.getUsername(), ""));
                span.setAttribute(AttributeKey.longKey("log.expire_days"), expireDays.longValue());
                if (StrUtil.isNotEmpty(form.getRequestIp())) {
                    span.setAttribute(AttributeKey.stringKey("log.request_ip"), form.getRequestIp());
                }
            }
            
            // 如果有初始内容，按行拆分并逐行存储
            if (StrUtil.isNotEmpty(form.getContent())) {
            String[] lines = form.getContent().split("\n");
            List<BusinessLog> logList = new ArrayList<>();
            
            for (int i = 0; i < lines.length; i++) {
                String lineContent = lines[i];
                // 如果开启了自动时间戳，添加时间戳前缀
                if (Boolean.TRUE.equals(form.getAutoTimestamp())) {
                    lineContent = "[" + DateUtil.formatDateTime(now) + "] " + lineContent;
                }
                
                BusinessLog businessLog = new BusinessLog();
                businessLog.setLogId(logId);
                businessLog.setModule(form.getModule());
                businessLog.setOperation(form.getOperation());
                businessLog.setUserId(form.getUserId());
                businessLog.setUsername(form.getUsername());
                businessLog.setContent(lineContent);
                businessLog.setLineNumber(i + 1);
                businessLog.setIsAppend(false);
                businessLog.setRequestIp(form.getRequestIp());
                businessLog.setTraceId(traceId); // 使用关联到追踪系统的traceId
                businessLog.setRemark(form.getRemark());
                
                // 在Span中添加日志事件
                if (span != null) {
                    addLogEventToSpan(span, lineContent, i + 1);
                }
                businessLog.setCreateTime(now);
                businessLog.setUpdateTime(now);
                businessLog.setExpireDays(expireDays);
                businessLog.setExpireDate(expireDate);
                businessLog.setStatus("ACTIVE");
                
                logList.add(businessLog);
            }
            
                // 批量发送日志到OpenObserve（使用根据过期时间选择的流）
                if (!logList.isEmpty()) {
                    String streamName = getStreamName(expireDays);
                    openObserveTemplate.postLogs(logList, streamName);
                    // 缓存到本地（用于后续查询和追加）
                    logIdStreamCache.put(logId, streamName);
                }
                log.info("创建业务日志成功，logId: {}, module: {}, operation: {}, 行数: {}, 过期天数: {}, 流名: {}, traceId: {}", 
                        logId, form.getModule(), form.getOperation(), lines.length, expireDays, getStreamName(expireDays), traceId);
                
                // 标记Span成功完成
                if (span != null) {
                    span.setAttribute(AttributeKey.longKey("log.total_lines"), (long) lines.length);
                    span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
                }
            } else {
                // 没有初始内容，创建一条空记录
                BusinessLog businessLog = new BusinessLog();
                businessLog.setLogId(logId);
                businessLog.setModule(form.getModule());
                businessLog.setOperation(form.getOperation());
                businessLog.setUserId(form.getUserId());
                businessLog.setUsername(form.getUsername());
                businessLog.setContent("");
                businessLog.setLineNumber(0);
                businessLog.setIsAppend(false);
                businessLog.setRequestIp(form.getRequestIp());
                businessLog.setTraceId(traceId); // 使用关联到追踪系统的traceId
                businessLog.setRemark(form.getRemark());
                businessLog.setCreateTime(now);
                businessLog.setUpdateTime(now);
                businessLog.setExpireDays(expireDays);
                businessLog.setExpireDate(expireDate);
                businessLog.setStatus("ACTIVE");
                
                String streamName = getStreamName(expireDays);
                openObserveTemplate.postLog(businessLog, streamName);
                // 缓存到本地（用于后续查询和追加）
                logIdStreamCache.put(logId, streamName);
                log.info("创建业务日志成功，logId: {}, module: {}, operation: {}, 过期天数: {}, 流名: {}, traceId: {}", 
                        logId, form.getModule(), form.getOperation(), expireDays, streamName, traceId);
                
                // 标记Span成功完成
                if (span != null) {
                    span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
                }
            }
        } catch (Exception e) {
            log.error("创建业务日志失败，logId: {}", logId, e);
            // 标记Span失败
            if (span != null) {
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
                span.recordException(e);
            }
            throw new RuntimeException("创建业务日志失败: " + e.getMessage());
        } finally {
            if (span != null) {
                span.end();
            }
            if (scope != null) {
                scope.close();
            }
        }
        if (!CollectionUtils.isEmpty(form.getStages())) {
            InitBusinessLogStageForm stageForm = new InitBusinessLogStageForm();
            stageForm.setLogId(logId);
            stageForm.setStages(form.getStages());
            initStages(stageForm);
        }
        return logId;
    }
    
    @Override
    public boolean appendLog(AppendBusinessLogForm form) {
        // 尝试获取当前Span，如果没有则创建新的Span用于追踪追加操作
        Span currentSpan = Span.current();
        Span appendSpan = null;
        Scope scope = null;
        
        // 如果当前没有活跃的Span，创建一个新的Span
        if (currentSpan == null || !currentSpan.getSpanContext().isValid()) {
            Tracer tracer = getTracer();
            if (tracer != null) {
                appendSpan = tracer.spanBuilder("business_log.append")
                        .setSpanKind(SpanKind.INTERNAL)
                        .setAttribute(AttributeKey.stringKey("log.id"), form.getLogId())
                        .startSpan();
                scope = appendSpan.makeCurrent();
            }
        }
        
        try {
            // 获取当前日志的最大行号
            Integer currentMaxLine = getLogTotalLines(form.getLogId());
            if (currentMaxLine == null) {
                currentMaxLine = 0;
            }
            
            // 按行拆分追加的内容
            String[] lines = form.getContent().split("\n");
            List<BusinessLog> appendLogList = new ArrayList<>();
            Date now = new Date();
            
            Span activeSpan = appendSpan != null ? appendSpan : currentSpan;
            
            for (int i = 0; i < lines.length; i++) {
                String lineContent = lines[i];
                // 如果开启了自动时间戳，添加时间戳前缀
                if (Boolean.TRUE.equals(form.getAutoTimestamp())) {
                    lineContent = "[" + DateUtil.formatDateTime(now) + "] " + lineContent;
                }
                
                BusinessLog appendLog = new BusinessLog();
                appendLog.setLogId(form.getLogId());
                appendLog.setContent(lineContent);
                appendLog.setLineNumber(currentMaxLine + i + 1);
                appendLog.setIsAppend(true);
                appendLog.setUpdateTime(now);
                appendLog.setStatus("ACTIVE");
                appendLog.setStageCode(form.getStageCode());
                appendLog.setStageName(form.getStageName());
                appendLog.setStageIndex(form.getStageIndex());
                appendLog.setStageProgress(form.getStageProgress());
                appendLog.setStageStatus(form.getStageStatus());
                appendLog.setStageCount(form.getStageCount());
                appendLog.setOverallProgress(form.getOverallProgress());
                appendLog.setStageEvent(form.getStageEvent());
                
                // 尝试从当前Span获取traceId
                if (activeSpan != null && activeSpan.getSpanContext().isValid()) {
                    appendLog.setTraceId(activeSpan.getSpanContext().getTraceId());
                }
                
                appendLogList.add(appendLog);
                
                // 在Span中添加日志事件
                if (activeSpan != null) {
                    addLogEventToSpan(activeSpan, lineContent, currentMaxLine + i + 1);
                }
            }
            
            // 批量发送追加日志到OpenObserve
            // 追加日志需要使用与原始日志相同的流，从日志记录中确定流名
            if (!appendLogList.isEmpty()) {
                String streamName = determineStreamByLogId(form.getLogId());
                
                // 如果还是找不到，使用默认流（30天）
                if (StrUtil.isEmpty(streamName)) {
                    streamName = getStreamName(30);
                    log.warn("无法确定logId {} 所在的流，使用默认流 {} 追加日志", form.getLogId(), streamName);
                }
                
                openObserveTemplate.postLogs(appendLogList, streamName);
                log.info("追加业务日志成功，logId: {}, 追加行数: {}, 流名: {}", form.getLogId(), lines.length, streamName);
                
                // 标记Span成功完成
                if (appendSpan != null) {
                    appendSpan.setAttribute(AttributeKey.longKey("log.append_lines"), (long) lines.length);
                    appendSpan.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("追加业务日志失败，logId: {}", form.getLogId(), e);
            // 标记Span失败
            if (appendSpan != null) {
                appendSpan.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
                appendSpan.recordException(e);
            }
            return false;
        } finally {
            if (appendSpan != null) {
                appendSpan.end();
            }
            if (scope != null) {
                scope.close();
            }
        }
    }
    
    @Override
    public List<BusinessLog> getLogByIdMultiLine(String logId) {
        if (StrUtil.isEmpty(logId)) {
            return Collections.emptyList();
        }
        
        try {
            Date now = new Date();
            Date beginTime = DateUtil.offsetDay(now, -7);
            Date endTime = DateUtil.offsetHour(now, 2);
            PageForm pageForm = new PageForm(1, 1000);

            List<BusinessLog> result = queryLogByStream(logId, beginTime, endTime, pageForm, determineStreamByLogId(logId));

            if (result.isEmpty()) {
                log.warn("⚠ 查询业务日志未找到数据，logId: {}，时间范围: {} ~ {}", 
                        logId, DateUtil.formatDateTime(beginTime), DateUtil.formatDateTime(endTime));
            } else {
                log.info("✓ 查询业务日志成功，logId: {}, 记录数: {}, 时间范围: {} ~ {}", 
                        logId, result.size(), DateUtil.formatDateTime(beginTime), DateUtil.formatDateTime(endTime));
            }
            return result;
        } catch (Exception e) {
            log.error("查询业务日志失败，logId: {}", logId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public String getLogByIdFullContent(String logId, Boolean formatted) {
        List<BusinessLog> logs = getLogByIdMultiLine(logId);
        
        if (logs.isEmpty()) {
            return "";
        }
        
        // 如果不需要格式化，直接返回原始日志内容
        if (formatted == null || !formatted) {
            StringBuilder rawContent = new StringBuilder();
            for (BusinessLog logEntry : logs) {
                rawContent.append(logEntry.getContent()).append("\n");
            }
            return rawContent.toString();
        }
        
        // 格式化输出：添加头部信息和行号
        StringBuilder fullContent = new StringBuilder();
        String separator = StrUtil.repeat("=", 80);
        fullContent.append(separator).append("\n");
        fullContent.append("业务日志ID: ").append(logId).append("\n");
        
        BusinessLog firstLog = logs.get(0);
        fullContent.append("业务模块: ").append(firstLog.getModule()).append("\n");
        fullContent.append("操作类型: ").append(firstLog.getOperation()).append("\n");
        if (StrUtil.isNotEmpty(firstLog.getUserId())) {
            fullContent.append("用户ID: ").append(firstLog.getUserId()).append("\n");
        }
        if (StrUtil.isNotEmpty(firstLog.getUsername())) {
            fullContent.append("用户名: ").append(firstLog.getUsername()).append("\n");
        }
        fullContent.append("创建时间: ").append(DateUtil.formatDateTime(firstLog.getCreateTime())).append("\n");
        fullContent.append("过期时间: ").append(DateUtil.formatDateTime(firstLog.getExpireDate())).append("\n");
        fullContent.append(separator).append("\n\n");
        
        // 追加每条日志内容（按行号顺序）
        for (BusinessLog logEntry : logs) {
            if (logEntry.getLineNumber() != null && logEntry.getLineNumber() > 0) {
                // 显示行号
                fullContent.append(String.format("%5d | ", logEntry.getLineNumber()));
            }
            fullContent.append(logEntry.getContent()).append("\n");
        }
        
        fullContent.append(separator).append("\n");
        fullContent.append("总行数: ").append(logs.size()).append("\n");
        
        return fullContent.toString();
    }
    
    @Override
    public List<BusinessLog> getLogByLineRange(String logId, Integer startLine, Integer endLine) {
        List<BusinessLog> allLogs = getLogByIdMultiLine(logId);
        
        if (allLogs.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 如果endLine为-1，表示到最后一行
        if (endLine == null || endLine == -1) {
            endLine = allLogs.size();
        }
        
        // 过滤行号范围
        int finalEndLine = endLine;
        int finalStartLine = startLine != null && startLine > 0 ? startLine : 1;
        
        return allLogs.stream()
                .filter(log -> {
                    Integer lineNum = log.getLineNumber();
                    return lineNum != null && lineNum >= finalStartLine && lineNum <= finalEndLine;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public Integer getLogTotalLines(String logId) {
        List<BusinessLog> logs = getLogByIdMultiLine(logId);
        if (logs.isEmpty()) {
            return 0;
        }
        
        // 获取最大行号
        return logs.stream()
                .map(BusinessLog::getLineNumber)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }
    
    @Override
    public DataPaging<BusinessLog> queryLogs(BusinessLogForm form) {
        // 设置默认分页参数
        if (form.getPageForm() == null) {
            form.setPageForm(new PageForm(1, 20));
        }
        
        try {
            Map<String, Object> params = BeanUtil.beanToMap(form.getBusinessLog());
            List<BusinessLog> allLogs = queryLogsFromAllStreams(params, form.getBeginTime(), form.getEndTime(), form.getPageForm());

            Map<String, BusinessLog> uniqueLogs = new HashMap<>();
            for (BusinessLog log : allLogs) {
                String key = log.getLogId() + "_" + (log.getLineNumber() != null ? log.getLineNumber() : 0);
                uniqueLogs.putIfAbsent(key, log);
            }

            List<BusinessLog> deduped = new ArrayList<>(uniqueLogs.values());
            deduped.sort(Comparator.comparing((BusinessLog log) -> {
                Date updateTime = log.getUpdateTime();
                return updateTime != null ? updateTime.getTime() : 0L;
            }).reversed());

            int total = deduped.size();
            int pageSize = form.getPageForm().getPageSize();
            int currPage = form.getPageForm().getCurrPage();
            int from = Math.max((currPage - 1) * pageSize, 0);
            int to = Math.min(from + pageSize, total);

            List<BusinessLog> pagedList = from < total ? deduped.subList(from, to) : Collections.emptyList();

            long totalPage = pageSize > 0 ? (total + pageSize - 1) / pageSize : 0L;
            log.info("分页查询业务日志成功，总记录数: {}, 当前页: {}, 每页: {}", total, currPage, pageSize);
            return new DataPaging<>(pagedList, (long) total, totalPage, form.getPageForm());
        } catch (Exception e) {
            log.error("分页查询业务日志失败", e);
            return new DataPaging<>(Collections.emptyList(), 0L, 0L, form.getPageForm());
        }
    }
    
    private List<BusinessLog> convertHitsToLogs(List<Map<String, Object>> hits) {
        if (hits == null || hits.isEmpty()) {
            return Collections.emptyList();
        }
        return hits.stream().map(map -> {
            JSONObject jsonObject = new JSONObject(map);
            return jsonObject.toJavaObject(BusinessLog.class);
        }).collect(Collectors.toList());
    }

    private List<BusinessLog> queryLogByStream(String logId, Date beginTime, Date endTime, PageForm pageForm, String preferredStream) {
        List<String> targetStreams = new ArrayList<>();
        if (StrUtil.isNotEmpty(preferredStream)) {
            targetStreams.add(preferredStream);
        }
        for (String stream : ALL_STREAM_NAMES) {
            if (!stream.equals(preferredStream)) {
                targetStreams.add(stream);
            }
        }

        Map<String, Object> paramTemplate = new HashMap<>();
        paramTemplate.put("logId", logId);

        for (String stream : targetStreams) {
            Map<String, Object> params = new HashMap<>(paramTemplate);
            params.put("streamName", stream);
            QueryResult queryResult = openObserveTemplate.selectLogs(
                    STATEMENT_SELECT_BY_LOG_ID_STREAM,
                    params,
                    beginTime,
                    endTime,
                    pageForm);
            List<BusinessLog> logs = convertHitsToLogs(queryResult.getHits());
            if (!logs.isEmpty()) {
                if (StrUtil.isEmpty(preferredStream)) {
                    logIdStreamCache.put(logId, stream);
                }
                return logs;
            }
        }
        return Collections.emptyList();
    }

    private List<BusinessLog> queryLogsFromAllStreams(Map<String, Object> baseParams, Date beginTime, Date endTime, PageForm pageForm) {
        List<BusinessLog> result = new ArrayList<>();
        for (String stream : ALL_STREAM_NAMES) {
            Map<String, Object> params = new HashMap<>(baseParams);
            params.put("streamName", stream);
            QueryResult queryResult = openObserveTemplate.selectLogs(
                    STATEMENT_SELECT_LOGS_BY_STREAM,
                    params,
                    beginTime,
                    endTime,
                    pageForm);
            result.addAll(convertHitsToLogs(queryResult.getHits()));
        }
        return result;
    }
    
    /**
     * 删除业务日志
     * 
     * ⚠️ 限制说明：
     * 1. OpenObserve不支持单条记录删除
     * 2. 此方法仅将日志状态标记为EXPIRED
     * 3. 实际数据删除需要等待流的TTL到期后由OpenObserve自动完成
     * 4. 如果需要立即删除，可以考虑手动清理整个流（不推荐）
     */
    @Override
    public boolean deleteLog(String logId) {
        if (StrUtil.isEmpty(logId)) {
            return false;
        }
        
        log.info("⚠️ 删除日志限制：OpenObserve不支持单条删除，仅标记状态为EXPIRED");
        log.info("实际数据将在流的TTL到期后自动删除");
        
        // OpenObserve不直接支持删除单条记录，通过更新状态为EXPIRED来标记删除
        // 需要找到原始日志所在的流，在该流中标记删除
        try {
            // 从日志记录中确定流名（不需要Redis）
            String streamName = determineStreamByLogId(logId);
            
            // 如果还是找不到，使用默认流（30天）
            if (StrUtil.isEmpty(streamName)) {
                streamName = getStreamName(30);
                log.warn("无法确定logId {} 所在的流，使用默认流 {}", logId, streamName);
            }
            
            BusinessLog deleteLog = new BusinessLog();
            deleteLog.setLogId(logId);
            deleteLog.setStatus("EXPIRED");
            deleteLog.setUpdateTime(new Date());
            
            openObserveTemplate.postLog(deleteLog, streamName);
            log.info("已标记日志为删除状态，logId: {}, 流: {}（实际数据将在流的TTL到期后自动删除）", logId, streamName);
            return true;
        } catch (Exception e) {
            log.error("删除业务日志失败，logId: {}", logId, e);
            return false;
        }
    }
    
    /**
     * 更新业务日志的过期时间（限制使用）
     * 
     * ⚠️ 限制说明：
     * 1. 由于日志按过期时间存储在OpenObserve的不同流中，更新过期时间不会立即迁移数据
     * 2. 原始日志仍在原流中，直到原流的TTL到期后自动删除
     * 3. 新标记的过期时间仅影响后续查询的状态判断
     * 
     * 建议：在创建日志时设置正确的过期时间，避免后续修改
     */
    @Override
    public boolean updateExpireTime(String logId, Integer expireDays) {
        if (StrUtil.isEmpty(logId) || expireDays == null || expireDays <= 0) {
            return false;
        }
        
        log.warn("⚠️ 更新过期时间限制：由于日志存储在按过期时间分组的流中，更新不会立即迁移数据");
        log.info("建议：在创建日志时设置正确的过期时间");
        
        try {
            BusinessLog updateLog = new BusinessLog();
            updateLog.setLogId(logId);
            updateLog.setExpireDays(expireDays);
            updateLog.setExpireDate(DateUtil.offsetDay(new Date(), expireDays));
            updateLog.setUpdateTime(new Date());
            
            // 注意：这只是标记新的过期时间，原始日志仍在原流中
            // 原始日志将在原流的TTL到期后自动删除
            String streamName = getStreamName(expireDays);
            openObserveTemplate.postLog(updateLog, streamName);
            log.info("已标记新的过期时间，logId: {}, expireDays: {}, 新流: {}（原始日志仍在原流中）", 
                    logId, expireDays, streamName);
            return true;
        } catch (Exception e) {
            log.error("更新业务日志过期时间失败，logId: {}", logId, e);
            return false;
        }
    }
    
    @Override
    public Map<String, String> generateTemporaryUrlParams(String logId, Integer expireMinutes) {
        if (StrUtil.isEmpty(logId)) {
            throw new RuntimeException("logId不能为空");
        }
        
        // 默认60分钟过期
        if (expireMinutes == null || expireMinutes <= 0) {
            expireMinutes = 60;
        }
        
        // 计算过期时间戳（秒）
        long expires = System.currentTimeMillis() / 1000 + expireMinutes * 60;
        
        // 生成token（使用MD5签名）
        String signString = logId + "_" + expires + "_" + TOKEN_SECRET;
        String token = MD5.create().digestHex(signString);
        
        // 生成AccessKeyId（简化版，实际可以使用配置的AccessKeyId）
        String accessKeyId = "LTAI" + IdUtil.fastSimpleUUID().substring(0, 16).toUpperCase();
        
        // 生成签名（类似OSS的签名算法）
        String signature = generateSignature(logId, expires, accessKeyId);
        
        // 将token存储到Redis（用于验证）
        if (redisService != null) {
            String cacheKey = TOKEN_CACHE_PREFIX + logId + ":" + token;
            redisService.setCacheObject(cacheKey, logId, Long.valueOf(expireMinutes), TimeUnit.MINUTES);
        }
        
        // 返回URL参数
        Map<String, String> params = new HashMap<>();
        params.put("expires", String.valueOf(expires));
        params.put("accessKeyId", accessKeyId);
        params.put("signature", signature);
        params.put("token", token);
        params.put("fileName", "raw-" + logId + ".log");
        
        log.info("生成临时访问URL参数成功，logId: {}, 过期时间: {}分钟", logId, expireMinutes);
        
        return params;
    }
    
    @Override
    public String getLogByToken(String logId, String token) {
        if (StrUtil.isEmpty(logId) || StrUtil.isEmpty(token)) {
            throw new RuntimeException("logId和token不能为空");
        }
        
        // 验证token（如果使用Redis）
        if (redisService != null) {
            String cacheKey = TOKEN_CACHE_PREFIX + logId + ":" + token;
            String cachedLogId = redisService.getCacheObject(cacheKey);
            if (StrUtil.isEmpty(cachedLogId) || !logId.equals(cachedLogId)) {
                throw new RuntimeException("Token无效或已过期");
            }
        }
        
        // 返回日志内容（通过token访问，默认返回原始格式）
        return getLogByIdFullContent(logId, false);
    }
    
    /**
     * 生成签名（类似OSS的签名算法）
     */
    private String generateSignature(String logId, long expires, String accessKeyId) {
        // 简化的签名算法（实际应该使用HMAC-SHA1等）
        String signString = "GET\n\n\n" + expires + "\n/" + logId + ".log";
        String signature = MD5.create().digestHex(signString + TOKEN_SECRET);
        // URL编码
        try {
            return java.net.URLEncoder.encode(signature, "UTF-8");
        } catch (Exception e) {
            return signature;
        }
    }
    
    @Override
    public String getLogIdByBusinessId(String businessType, String businessId) {
        if (StrUtil.isBlank(businessType) || StrUtil.isBlank(businessId)) {
            log.error("businessType和businessId不能为空");
            return null;
        }
        
        try {
            // 通过 businessType 和 businessId 查询日志
            // 返回最新的一条记录的 logId
            BusinessLogForm form = new BusinessLogForm();
            
            // 创建 BusinessLog 对象并设置查询条件
            BusinessLog queryLog = new BusinessLog();
            queryLog.setBusinessType(businessType);
            queryLog.setBusinessId(businessId);
            form.setBusinessLog(queryLog);
            
            // 设置分页参数，只查询第一条记录
            PageForm pageForm = new PageForm();
            pageForm.setCurrPage(1);
            pageForm.setPageSize(1);
            form.setPageForm(pageForm);
            
            // 查询日志
            DataPaging<BusinessLog> dataPaging = queryLogs(form);
            
            if (dataPaging != null && dataPaging.getContents() != null && !dataPaging.getContents().isEmpty()) {
                BusinessLog firstLog = dataPaging.getContents().get(0);
                String logId = firstLog.getLogId();
                log.debug("通过businessType={}, businessId={}查询到logId={}", businessType, businessId, logId);
                return logId;
            }
            
            log.warn("未找到对应的日志: businessType={}, businessId={}", businessType, businessId);
            return null;
            
        } catch (Exception e) {
            log.error("通过businessType和businessId查询logId失败", e);
            return null;
        }
    }

    @Override
    public BusinessLogStageSnapshot initStages(InitBusinessLogStageForm form) {
        if (form == null || StrUtil.isBlank(form.getLogId())) {
            throw new IllegalArgumentException("logId不能为空");
        }
        BusinessLogStageTracker tracker = getOrCreateStageTracker(form.getLogId());
        BusinessLogStageSnapshot snapshot = tracker.initStages(form.getStages());
        log.info("初始化业务日志阶段，logId: {}, 阶段数: {}", form.getLogId(),
                snapshot != null ? snapshot.getStageCount() : 0);
        return snapshot;
    }

    @Override
    public BusinessLogStageSnapshot updateStage(BusinessLogStageUpdateForm form) {
        if (form == null || StrUtil.isBlank(form.getLogId())) {
            throw new IllegalArgumentException("logId不能为空");
        }
        BusinessLogStageTracker tracker = getOrCreateStageTracker(form.getLogId());
        BusinessLogStageSnapshot snapshot = tracker.update(form);
        if (snapshot != null && Boolean.TRUE.equals(form.getAppendLog())) {
            BusinessLogStageItem activeStage = resolveActiveStage(snapshot);
            AppendBusinessLogForm appendForm = new AppendBusinessLogForm();
            appendForm.setLogId(form.getLogId());
            appendForm.setAutoTimestamp(form.getAutoTimestamp());
            appendForm.setStageEvent(true);
            appendForm.setStageCount(snapshot.getStageCount());
            appendForm.setOverallProgress(snapshot.getOverallProgress());
            if (activeStage != null) {
                appendForm.setStageCode(activeStage.getStageCode());
                appendForm.setStageName(activeStage.getStageName());
                appendForm.setStageIndex(activeStage.getOrderIndex());
                appendForm.setStageProgress(activeStage.getProgress());
                appendForm.setStageStatus(activeStage.getStatus());
            } else {
                appendForm.setStageName(form.getStageName());
                appendForm.setStageIndex(form.getStageIndex());
                appendForm.setStageProgress(form.getProgress());
                appendForm.setStageStatus(form.getStatus());
            }
            String stageContent = StrUtil.isNotBlank(form.getContent())
                    ? form.getContent()
                    : buildStageLogMessage(activeStage, form, snapshot);
            appendForm.setContent(stageContent);
            appendLog(appendForm);
        }
        log.debug("更新业务日志阶段，logId: {}, 当前阶段: {}, 总进度: {}%", form.getLogId(),
                snapshot != null ? snapshot.getActiveStageName() : "-", 
                snapshot != null ? snapshot.getOverallProgress() : 0);
        return snapshot;
    }

    @Override
    public BusinessLogStageSnapshot getStageSnapshot(String logId) {
        if (StrUtil.isBlank(logId)) {
            return null;
        }
        BusinessLogStageTracker tracker = stageTrackers.get(logId);
        return tracker != null ? tracker.snapshot() : null;
    }

    private BusinessLogStageTracker getOrCreateStageTracker(String logId) {
        return stageTrackers.computeIfAbsent(logId, BusinessLogStageTracker::new);
    }

    private BusinessLogStageItem resolveActiveStage(BusinessLogStageSnapshot snapshot) {
        if (snapshot == null || CollectionUtils.isEmpty(snapshot.getStages())) {
            return null;
        }
        if (snapshot.getActiveStageIndex() != null) {
            for (BusinessLogStageItem item : snapshot.getStages()) {
                if (Objects.equals(item.getOrderIndex(), snapshot.getActiveStageIndex())) {
                    return item;
                }
            }
        }
        if (StrUtil.isNotBlank(snapshot.getActiveStageName())) {
            for (BusinessLogStageItem item : snapshot.getStages()) {
                if (snapshot.getActiveStageName().equals(item.getStageName())) {
                    return item;
                }
            }
        }
        return snapshot.getStages().get(0);
    }

    private String buildStageLogMessage(BusinessLogStageItem stage,
                                        BusinessLogStageUpdateForm form,
                                        BusinessLogStageSnapshot snapshot) {
        String stageName = stage != null ? stage.getStageName() : StrUtil.emptyToDefault(form.getStageName(), "阶段");
        String status = StrUtil.emptyToDefault(
                stage != null ? stage.getStatus() : form.getStatus(),
                "RUNNING");
        int progress = form.getProgress() != null ? form.getProgress()
                : stage != null && stage.getProgress() != null ? stage.getProgress()
                : snapshot != null && snapshot.getOverallProgress() != null ? snapshot.getOverallProgress() : 0;
        String message = StrUtil.emptyToDefault(form.getMessage(), "");
        return StrUtil.trim(String.format("阶段[%s] 状态: %s, 进度: %d%% %s",
                stageName, status, progress, message));
    }

    private static int clampProgress(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, value));
    }

    /**
     * 阶段进度跟踪器
     */
    private static class BusinessLogStageTracker {
        private final String logId;
        private final LinkedHashMap<String, BusinessLogStageItem> stageMap = new LinkedHashMap<>();
        private final AtomicLong version = new AtomicLong(0);
        private int stageCount;
        private int overallProgress;
        private String overallStatus = "WAITING";
        private Integer activeStageIndex;
        private String activeStageName;
        private Date updateTime;

        private BusinessLogStageTracker(String logId) {
            this.logId = logId;
        }

        private synchronized BusinessLogStageSnapshot initStages(List<BusinessLogStageItem> stages) {
            stageMap.clear();
            if (!CollectionUtils.isEmpty(stages)) {
                List<BusinessLogStageItem> normalized = new ArrayList<>();
                int fallbackOrder = 1;
                for (BusinessLogStageItem item : stages) {
                    BusinessLogStageItem copy = new BusinessLogStageItem();
                    if (item != null) {
                        BeanUtil.copyProperties(item, copy);
                    }
                    int orderIndex = item != null && item.getOrderIndex() != null ? item.getOrderIndex() : fallbackOrder;
                    copy.setOrderIndex(orderIndex);
                    copy.setStageCode(StrUtil.isNotBlank(copy.getStageCode()) ? copy.getStageCode() : "stage-" + orderIndex);
                    copy.setStageName(StrUtil.isNotBlank(copy.getStageName()) ? copy.getStageName() : "阶段 " + orderIndex);
                    copy.setProgress(copy.getProgress() == null ? 0 : clampProgress(copy.getProgress()));
                    copy.setStatus(StrUtil.isNotBlank(copy.getStatus()) ? copy.getStatus().toUpperCase() : "WAITING");
                    copy.setUpdateTime(new Date());
                    normalized.add(copy);
                    fallbackOrder++;
                }
                normalized.sort(Comparator.comparingInt(item -> item.getOrderIndex() == null ? Integer.MAX_VALUE : item.getOrderIndex()));
                int index = 1;
                for (BusinessLogStageItem stage : normalized) {
                    if (stage.getOrderIndex() == null) {
                        stage.setOrderIndex(index);
                    }
                    stageMap.put(stage.getStageCode(), stage);
                    index++;
                }
            }
            stageCount = stageMap.size();
            overallProgress = calcOverallProgress();
            overallStatus = computeOverallStatus();
            BusinessLogStageItem first = stageMap.values().stream().findFirst().orElse(null);
            activeStageIndex = first != null ? first.getOrderIndex() : null;
            activeStageName = first != null ? first.getStageName() : null;
            updateTime = new Date();
            version.incrementAndGet();
            return snapshotInternal();
        }

        private synchronized BusinessLogStageSnapshot update(BusinessLogStageUpdateForm form) {
            BusinessLogStageItem stage = locateOrCreateStage(form);
            if (stage == null) {
                return snapshotInternal();
            }
            if (StrUtil.isNotBlank(form.getStageName())) {
                stage.setStageName(form.getStageName());
            }
            if (form.getProgress() != null) {
                stage.setProgress(clampProgress(form.getProgress()));
            } else if (stage.getProgress() == null) {
                stage.setProgress(0);
            }
            if (StrUtil.isNotBlank(form.getStatus())) {
                stage.setStatus(form.getStatus().toUpperCase());
            } else if (stage.getStatus() == null) {
                stage.setStatus("RUNNING");
            }
            if (StrUtil.isNotBlank(form.getMessage())) {
                stage.setMessage(form.getMessage());
            }
            stage.setUpdateTime(new Date());
            activeStageIndex = stage.getOrderIndex();
            activeStageName = stage.getStageName();
            if (form.getOverallProgress() != null) {
                overallProgress = clampProgress(form.getOverallProgress());
            } else {
                overallProgress = calcOverallProgress();
            }
            overallStatus = computeOverallStatus();
            updateTime = new Date();
            version.incrementAndGet();
            return snapshotInternal();
        }

        private synchronized BusinessLogStageSnapshot snapshot() {
            return snapshotInternal();
        }

        private BusinessLogStageItem locateOrCreateStage(BusinessLogStageUpdateForm form) {
            BusinessLogStageItem target = null;
            if (StrUtil.isNotBlank(form.getStageCode())) {
                target = stageMap.get(form.getStageCode());
            }
            if (target == null && form.getStageIndex() != null) {
                target = stageMap.values().stream()
                        .filter(item -> Objects.equals(item.getOrderIndex(), form.getStageIndex()))
                        .findFirst()
                        .orElse(null);
            }
            if (target == null && StrUtil.isNotBlank(form.getStageCode())) {
                target = buildStage(form, form.getStageCode());
            }
            if (target == null) {
                String generatedCode = StrUtil.isNotBlank(form.getStageCode())
                        ? form.getStageCode()
                        : "stage-" + (stageMap.size() + 1);
                target = buildStage(form, generatedCode);
            }
            return target;
        }

        private BusinessLogStageItem buildStage(BusinessLogStageUpdateForm form, String stageCode) {
            BusinessLogStageItem stage = new BusinessLogStageItem();
            stage.setStageCode(stageCode);
            int orderIndex = form.getStageIndex() != null ? form.getStageIndex() : stageMap.size() + 1;
            stage.setOrderIndex(orderIndex);
            stage.setStageName(StrUtil.isNotBlank(form.getStageName()) ? form.getStageName() : "阶段 " + orderIndex);
            stage.setProgress(form.getProgress() != null ? clampProgress(form.getProgress()) : 0);
            stage.setStatus(StrUtil.isNotBlank(form.getStatus()) ? form.getStatus().toUpperCase() : "WAITING");
            stage.setMessage(form.getMessage());
            stage.setUpdateTime(new Date());
            stageMap.put(stage.getStageCode(), stage);
            reorder();
            stageCount = stageMap.size();
            return stageMap.get(stage.getStageCode());
        }

        private void reorder() {
            List<BusinessLogStageItem> sorted = stageMap.values().stream()
                    .sorted(Comparator.comparingInt(item -> item.getOrderIndex() == null ? Integer.MAX_VALUE : item.getOrderIndex()))
                    .collect(Collectors.toList());
            stageMap.clear();
            int index = 1;
            for (BusinessLogStageItem item : sorted) {
                if (item.getOrderIndex() == null) {
                    item.setOrderIndex(index);
                }
                stageMap.put(item.getStageCode(), item);
                index++;
            }
        }

        private int calcOverallProgress() {
            if (stageMap.isEmpty()) {
                return 0;
            }
            int total = stageMap.values().stream()
                    .map(item -> item.getProgress() == null ? 0 : clampProgress(item.getProgress()))
                    .reduce(0, Integer::sum);
            return total / stageMap.size();
        }

        private String computeOverallStatus() {
            if (stageMap.isEmpty()) {
                return "WAITING";
            }
            if (stageMap.values().stream().anyMatch(item -> "FAILED".equalsIgnoreCase(
                    StrUtil.emptyToDefault(item.getStatus(), "WAITING")))) {
                return "FAILED";
            }
            boolean allDone = stageMap.values().stream().allMatch(item ->
                    "DONE".equalsIgnoreCase(StrUtil.emptyToDefault(item.getStatus(), "WAITING")));
            if (allDone) {
                return "DONE";
            }
            boolean anyRunning = stageMap.values().stream().anyMatch(item ->
                    "RUNNING".equalsIgnoreCase(StrUtil.emptyToDefault(item.getStatus(), "WAITING")));
            if (anyRunning) {
                return "RUNNING";
            }
            return "WAITING";
        }

        private BusinessLogStageSnapshot snapshotInternal() {
            BusinessLogStageSnapshot snapshot = new BusinessLogStageSnapshot();
            snapshot.setLogId(logId);
            snapshot.setStageCount(stageCount);
            snapshot.setOverallProgress(overallProgress);
            snapshot.setOverallStatus(overallStatus);
            snapshot.setActiveStageIndex(activeStageIndex);
            snapshot.setActiveStageName(activeStageName);
            snapshot.setUpdateTime(updateTime != null ? (Date) updateTime.clone() : null);
            snapshot.setVersion(version.get());
            List<BusinessLogStageItem> items = stageMap.values().stream()
                    .sorted(Comparator.comparingInt(item -> item.getOrderIndex() == null ? Integer.MAX_VALUE : item.getOrderIndex()))
                    .map(this::copyStage)
                    .collect(Collectors.toList());
            snapshot.setStages(items);
            return snapshot;
        }

        private BusinessLogStageItem copyStage(BusinessLogStageItem source) {
            BusinessLogStageItem target = new BusinessLogStageItem();
            BeanUtil.copyProperties(source, target);
            if (source.getUpdateTime() != null) {
                target.setUpdateTime((Date) source.getUpdateTime().clone());
            }
            return target;
        }
    }
}

