package org.springframework.data.influx;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @author wesley
 */
public class InfluxDateUtil {

    public static String toUtcTimeStr(Date time) {
        LocalDateTime localDateTime = LocalDateTimeUtil.of(time).atOffset(ZonedDateTime.now().getOffset()).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return LocalDateTimeUtil.format(localDateTime, DatePattern.NORM_DATETIME_PATTERN);
    }

    public static Date formUtcToDate(String timeStr) {
        return Date.from(DateUtil.parseUTC(timeStr).toInstant().atOffset(ZonedDateTime.now().getOffset()).toInstant());
    }
}
