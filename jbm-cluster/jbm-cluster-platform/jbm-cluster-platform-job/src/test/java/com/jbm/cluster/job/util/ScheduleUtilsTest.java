package com.jbm.cluster.job.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(20)
class ScheduleUtilsTest {

    @Test
    void whiteList_allowsJbmPackage() {
        assertTrue(ScheduleUtils.whiteList("com.jbm.cluster.job.service.FooService.run()"));
    }

    @Test
    void whiteList_singleSegmentAlwaysTrue() {
        assertTrue(ScheduleUtils.whiteList("run()"));
    }

    @Test
    void whiteList_rejectsNonJbmMultiSegment() {
        assertFalse(ScheduleUtils.whiteList("com.other.Evil.run()"));
    }
}
