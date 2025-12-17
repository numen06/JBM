package com.jbm.cluster.common.basic.module.log;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.slf4j.MDC;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 基于当前 MDC 解析出来的业务日志快照。
 */
@Getter
public class BusinessLogSnapshot {

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

    public static BusinessLogSnapshot fromMdc(String defaultSource, Integer overrideExpireDays,
                                              Supplier<String> callerSupplier, int defaultExpireDays) {
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        String threadLogId = BusinessLogContext.currentLogId();

        String logId = firstNonBlankValue(threadLogId, firstNonBlank(mdc, "businessLogId"),
                firstNonBlank(mdc, "traceId"));
        BusinessLogContext cached = BusinessLogContext.getCached(logId);

        String fallbackType = defaultSource;
        String fallbackId = callerSupplier != null ? callerSupplier.get() : defaultSource;

        String businessType = StrUtil.blankToDefault(cached != null ? cached.getBusinessType() : null, fallbackType);
        String businessId = StrUtil.blankToDefault(cached != null ? cached.getBusinessId() : null,
                StrUtil.blankToDefault(logId, fallbackId));

        String source = StrUtil.blankToDefault(cached != null ? cached.getSource() : null, defaultSource);

        Integer expireDays = cached != null && cached.getExpireDays() != null
                ? cached.getExpireDays()
                : (overrideExpireDays != null ? overrideExpireDays : defaultExpireDays);

        String operator = cached != null ? cached.getOperator() : null;
        String operatorId = cached != null ? cached.getOperatorId() : null;
        String tenantId = cached != null ? cached.getTenantId() : null;
        String appId = cached != null ? cached.getAppId() : source;

        // 默认不加时间戳，因为log本身已经有时间戳了
        boolean autoTimestamp = cached != null && Boolean.TRUE.equals(cached.getAutoTimestamp());

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

    public boolean isFinished() {
        return finished;
    }

    public boolean valid() {
        return StrUtil.isNotBlank(businessType) && StrUtil.isNotBlank(businessId);
    }

    public String decorate(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }
        if (!autoTimestamp) {
            return content;
        }
        return StrUtil.format("[{}] {}", DateUtil.format(DateUtil.date(), DatePattern.NORM_DATETIME_MS_FORMAT), content);
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

