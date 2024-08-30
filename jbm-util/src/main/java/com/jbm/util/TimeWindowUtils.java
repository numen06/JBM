package com.jbm.util;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.jbm.util.time.TimeWindow;
import lombok.Data;

import java.time.*;
import java.util.Date;

/**
 * 时间窗口工具类
 *
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
    public static Date getTimeWindowStart(Date currentTime, int intervalLength) {
        LocalDateTime localDateTime = DateTime.of(currentTime).toLocalDateTime();
        // 计算当前时间与最近的区间起始时间之间的分钟数
        int minutesSinceStartOfInterval = localDateTime.getMinute() % intervalLength;
        int secondsSinceStartOfInterval = localDateTime.getSecond();
        int nanosSinceStartOfInterval = localDateTime.getNano();

        // 如果当前时间不在区间的末尾，则减少当前时间到最近的区间的起始时间
        if (minutesSinceStartOfInterval != 0 || secondsSinceStartOfInterval != 0 || nanosSinceStartOfInterval != 0) {
            localDateTime = localDateTime.minusMinutes(minutesSinceStartOfInterval).minusSeconds(secondsSinceStartOfInterval).withNano(0);
            // 转换为java.util.Date
            // 默认时区，通常是系统时区
            ZoneId defaultZoneId = ZoneId.systemDefault();
            // 转换为ZonedDateTime
            ZonedDateTime zonedDateTime = localDateTime.atZone(defaultZoneId);
            // 转换为java.util.Date
            return DateTime.from(zonedDateTime.toInstant());
        } else {
            // 如果当前时间正好在区间的末尾，则保持不变
            return DateTime.of(currentTime);
        }
    }

    /**
     * 获取时间窗口的结束时间
     *
     * @param currentTime
     * @param intervalLength
     * @return
     */
    public static Date getTimeWindowEnd(Date currentTime, int intervalLength) {
        // 计算当前时间所在的区间
        Date intervalStart = getTimeWindowStart(currentTime, intervalLength);
        return DateUtil.offsetMinute(intervalStart,intervalLength);
    }

    /**
     * 获取当前时间所在的区间
     *
     * @param currentTime
     * @param intervalLength
     * @return
     */
    public static TimeWindow getTimeWindow(Date currentTime, int intervalLength) {
        // 计算当前时间所在的区间
        Date intervalStart = getTimeWindowStart(currentTime, intervalLength);
        Date intervalEnd = DateUtil.offsetMinute(intervalStart, intervalLength);
        return new TimeWindow(intervalStart, intervalEnd);
    }
}
