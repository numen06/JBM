package com.jbm.cluster.common.basic.module.log;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.MDC;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 统一的业务日志上下文管理。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessLogContext {

    private static final ThreadLocal<String> CURRENT_LOG_ID = new ThreadLocal<>();
    private static final ConcurrentMap<String, BusinessLogContext> CONTEXT_CACHE = new ConcurrentHashMap<>();

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

    public static BusinessLogContextBuilder log() {
        return BusinessLogContext.builder();
    }

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
        CONTEXT_CACHE.computeIfPresent(cacheKey(effectiveLogId), (key, ctx) -> {
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

    public static BusinessLogContext getCached(String logId) {
        if (StrUtil.isBlank(logId)) {
            return null;
        }
        return CONTEXT_CACHE.get(cacheKey(logId));
    }

    public static void clearByLockKey(String lockKey) {
        if (StrUtil.isBlank(lockKey)) {
            return;
        }
        CONTEXT_CACHE.remove(lockKey);
    }

    public static String currentLogId() {
        String value = CURRENT_LOG_ID.get();
        if (StrUtil.isNotBlank(value)) {
            return value;
        }
        return MDC.get("businessLogId");
    }

    public static String cacheKey(String logId) {
        return BusinessLogTraceUtils.traceLockKey(logId);
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
        String key = cacheKey(logId);
        BusinessLogContext merged = CONTEXT_CACHE.compute(key, (cacheKey, existing) -> mergeContext(existing, incoming));
        if (merged == null) {
            merged = copyContext(incoming);
            CONTEXT_CACHE.put(key, merged);
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
}

