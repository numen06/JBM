package com.jbm.cluster.job.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cron 校验与下次触发时间（Spring CronExpression，无 Quartz）。
 */
@Timeout(20)
class CronUtilsTest {

    @Test
    void isValid_acceptsSixFieldCron() {
        assertTrue(CronUtils.isValid("0/5 * * * * ?"));
        assertTrue(CronUtils.isValid("0 0 12 * * ?"));
    }

    @Test
    void isValid_rejectsInvalidExpression() {
        assertFalse(CronUtils.isValid("not a cron"));
        assertFalse(CronUtils.isValid(""));
    }

    @Test
    void getInvalidMessage_nullWhenValid() {
        assertNull(CronUtils.getInvalidMessage("0 0 * * * ?"));
        assertNotNull(CronUtils.getInvalidMessage("invalid"));
    }

    @Test
    void getNextExecution_returnsFutureDate() {
        Date next = CronUtils.getNextExecution("0/10 * * * * ?");
        assertNotNull(next);
        assertTrue(next.getTime() > System.currentTimeMillis() - 60_000L);
    }

    @Test
    void nextInstant_strictlyAfterBase() {
        Instant base = Instant.parse("2026-05-16T10:00:00Z");
        Instant next = CronUtils.nextInstant("0 0 * * * ?", base);
        assertTrue(next.isAfter(base));
    }
}
