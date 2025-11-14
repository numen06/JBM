package com.jbm.cluster.common.basic.module.log;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.jbm.cluster.common.basic.module.JbmBusinessLogTemplate;
import jbm.framework.spring.config.SpringContextHolder;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于 MDC 的业务日志采集 Appender。
 *
 * <p>只要日志上下文中设置了 {@code businessType} 与 {@code businessId}，
 * 便会自动将每一条日志透传给 {@link JbmBusinessLogTemplate}，无需显式调用模板。</p>
 *
 * <p>触发逻辑：</p>
 * <ul>
 *     <li>首条命中日志会执行 {@code startLog}</li>
 *     <li>后续日志自动执行 {@code appendLog}</li>
 *     <li>当 MDC 中设置 {@code businessLogFinished=true} 时会回收上下文，允许后续重新开始</li>
 * </ul>
 *
 * @author wesley
 */
public class BusinessLogMdcAppender extends AppenderBase<ILoggingEvent> {

    private static final String MDC_BUSINESS_TYPE = "businessType";
    private static final String MDC_BUSINESS_ID = "businessId";
    private static final String MDC_LOG_ID = "businessLogId";
    private static final String MDC_FINISHED = "businessLogFinished";

    private static final int MAX_TRACKED_CONTEXT = 2048;
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 5 * 60 * 1000;

    private final ConcurrentMap<String, Long> contextTimeline = new ConcurrentHashMap<>();

    private JbmBusinessLogTemplate businessLogTemplate;
    private long idleTimeoutMs = DEFAULT_IDLE_TIMEOUT_MS;
    private boolean includeThrowable = true;

    public void setIdleTimeoutMs(long idleTimeoutMs) {
        if (idleTimeoutMs > 0) {
            this.idleTimeoutMs = idleTimeoutMs;
        }
    }

    public void setIncludeThrowable(boolean includeThrowable) {
        this.includeThrowable = includeThrowable;
    }

    @Override
    protected void append(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc == null || mdc.isEmpty()) {
            return;
        }
        String businessType = StrUtil.trim(mdc.get(MDC_BUSINESS_TYPE));
        String businessId = StrUtil.trim(mdc.get(MDC_BUSINESS_ID));
        if (StrUtil.hasBlank(businessType, businessId)) {
            return;
        }

        JbmBusinessLogTemplate template = resolveTemplate();
        if (template == null) {
            return;
        }

        String contextKey = buildContextKey(businessType, businessId, mdc.get(MDC_LOG_ID));
        boolean finished = Boolean.parseBoolean(mdc.get(MDC_FINISHED));
        if (finished) {
            contextTimeline.remove(contextKey);
            return;
        }

        String payload = buildPayload(event);
        if (StrUtil.isBlank(payload)) {
            return;
        }

        boolean isNew = contextTimeline.put(contextKey, System.currentTimeMillis()) == null;
        if (isNew) {
            template.startLog(payload);
        } else {
            template.appendLog(payload);
        }
        cleanupExpiredContexts();
    }

    private JbmBusinessLogTemplate resolveTemplate() {
        if (businessLogTemplate != null) {
            return businessLogTemplate;
        }
        try {
            businessLogTemplate = SpringContextHolder.getBean(JbmBusinessLogTemplate.class);
        } catch (Exception ex) {
            addWarn("无法获取 JbmBusinessLogTemplate Bean，MDC 业务日志将被跳过", ex);
        }
        return businessLogTemplate;
    }

    private String buildContextKey(String businessType, String businessId, String logId) {
        if (StrUtil.isNotBlank(logId)) {
            return logId;
        }
        return businessType + "::" + businessId;
    }

    private String buildPayload(ILoggingEvent event) {
        StringBuilder builder = new StringBuilder();
        builder.append('[')
                .append(DateUtil.format(new Date(event.getTimeStamp()), DatePattern.NORM_DATETIME_MS_FORMAT))
                .append("]");
        Level level = event.getLevel();
        if (level != null) {
            builder.append('[').append(level.levelStr).append(']');
        }
        if (StrUtil.isNotBlank(event.getLoggerName())) {
            builder.append('[').append(event.getLoggerName()).append(']');
        }
        builder.append(' ').append(event.getFormattedMessage());

        if (includeThrowable) {
            IThrowableProxy throwableProxy = event.getThrowableProxy();
            if (throwableProxy != null) {
                builder.append(System.lineSeparator())
                        .append(throwableProxy.getClassName())
                        .append(": ")
                        .append(StrUtil.nullToEmpty(throwableProxy.getMessage()));
                StackTraceElementProxy[] stackTrace = throwableProxy.getStackTraceElementProxyArray();
                if (stackTrace != null) {
                    int limit = Math.min(stackTrace.length, 6);
                    for (int i = 0; i < limit; i++) {
                        builder.append(System.lineSeparator())
                                .append("  at ").append(stackTrace[i].toString());
                    }
                    if (stackTrace.length > limit) {
                        builder.append(System.lineSeparator()).append("  ...");
                    }
                }
            }
        }
        return builder.toString();
    }

    private void cleanupExpiredContexts() {
        if (contextTimeline.size() <= MAX_TRACKED_CONTEXT) {
            return;
        }
        long threshold = System.currentTimeMillis() - idleTimeoutMs;
        contextTimeline.entrySet().removeIf(entry -> entry.getValue() < threshold);
    }

    @Override
    public void stop() {
        super.stop();
        contextTimeline.clear();
        businessLogTemplate = null;
    }
}

