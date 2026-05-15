package com.jbm.cluster.job.util;

import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.util.Date;

/**
 * cron 表达式工具类（基于 Spring {@link CronExpression}，不依赖 Quartz）。
 */
public final class CronUtils {

    private CronUtils() {
    }

    /**
     * 返回一个布尔值代表一个给定的Cron表达式的有效性
     */
    public static boolean isValid(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 无效时返回表达式错误描述,如果有效返回null
     */
    public static String getInvalidMessage(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
            return null;
        } catch (Exception pe) {
            return pe.getMessage();
        }
    }

    /**
     * 下次执行时间（相对给定时间点之后）
     */
    public static Date getNextExecution(String cronExpression) {
        Instant next = nextInstant(cronExpression, Instant.now());
        return Date.from(next);
    }

    /**
     * 下次执行时间点
     */
    public static Instant nextInstant(String cronExpression, Instant base) {
        CronExpression cron = CronExpression.parse(cronExpression);
        Instant next = cron.next(base);
        if (next == null) {
            throw new IllegalArgumentException("Cron 表达式无后续触发时间: " + cronExpression);
        }
        return next;
    }
}
