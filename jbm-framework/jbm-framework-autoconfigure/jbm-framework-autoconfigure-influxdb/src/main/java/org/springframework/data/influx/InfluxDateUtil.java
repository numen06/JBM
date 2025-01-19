package org.springframework.data.influx;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;

import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author wesley
 */
public class InfluxDateUtil {

    public final static TimeZone UTC = TimeZone.getTimeZone("UTC");

    private final static String INFLUX_UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSS'Z'";

    public static String toUtcTimeStr(Date time) {
        return DateUtil.format(DateTime.of(time).setTimeZone(UTC), DatePattern.UTC_FORMAT);
//        return DateUtil.format(DateTime.of(time).setTimeZone(UTC), INFLUX_UTC_FORMAT);
    }

    public static Date formUtcToDate(String utcString) {
        String timeStr = utcString;
        if (StrUtil.contains(utcString, 'Z')) {
            int dotIndex = utcString.indexOf(CharUtil.DOT);
            if (dotIndex > 0 && (utcString.length() - dotIndex) > 4) {
                timeStr = StrUtil.sub(utcString, 0, dotIndex + 3) + "Z";
            }
        }
        return DateUtil.parseUTC(timeStr).setTimeZone(TimeZone.getDefault()).toJdkDate();
    }


}
