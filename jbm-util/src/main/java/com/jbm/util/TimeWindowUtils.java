package com.jbm.util;

import com.jbm.util.time.TimeWindow;
import lombok.Data;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * 时间窗口工具类
 * @author wesley
 */
@Data
public class TimeWindowUtils {

    /**
     * 获取时间窗口的起始时间
     *
     * @param currentTime
     * @param intervalLength
     * @return
     */
    public static LocalTime getTimeWindowStart(LocalTime currentTime, int intervalLength) {
        // 计算当前时间与最近的区间起始时间之间的分钟数
        int minutesSinceStartOfInterval = currentTime.getMinute() % intervalLength;
        int secondsSinceStartOfInterval = currentTime.getSecond();
        int nanosSinceStartOfInterval = currentTime.getNano();

        // 如果当前时间不在区间的末尾，则减少当前时间到最近的区间的起始时间
        if (minutesSinceStartOfInterval != 0 || secondsSinceStartOfInterval != 0 || nanosSinceStartOfInterval != 0) {
            return currentTime.minus(minutesSinceStartOfInterval, ChronoUnit.MINUTES)
                    .minus(secondsSinceStartOfInterval, ChronoUnit.SECONDS)
                    .withNano(0);
        } else {
            // 如果当前时间正好在区间的末尾，则保持不变
            return currentTime;
        }
    }

    /**
     * 获取时间窗口的结束时间
     *
     * @param currentTime
     * @param intervalLength
     * @return
     */
    public static LocalTime getTimeWindowEnd(LocalTime currentTime, int intervalLength) {
        // 计算当前时间所在的区间
        LocalTime intervalStart = getTimeWindowStart(currentTime, intervalLength);
        return intervalStart.plusMinutes(intervalLength);
    }

    /**
     * 获取当前时间所在的区间
     *
     * @param currentTime
     * @param intervalLength
     * @return
     */
    public static TimeWindow getTimeWindow(LocalTime currentTime, int intervalLength) {
        // 计算当前时间所在的区间
        LocalTime intervalStart = getTimeWindowStart(currentTime, intervalLength);
        LocalTime intervalEnd = intervalStart.plusMinutes(intervalLength);
        return new TimeWindow(intervalStart, intervalEnd);
    }
}
