package com.jbm.util;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 时间工具类
 *
 * @author Wesley
 *
 * <pre>
 * 字母  日期或时间元素  表示  示例
 * G  Era 标志符  Text  AD
 * y  年  Year  1996; 96
 * M  年中的月份  Month  July; Jul; 07
 * w  年中的周数  Number  27
 * W  月份中的周数  Number  2
 * D  年中的天数  Number  189
 * d  月份中的天数  Number  10
 * F  月份中的星期  Number  2
 * E  星期中的天数  Text  Tuesday; Tue
 * a  Am/pm 标记  Text  PM
 * H  一天中的小时数（0-23）  Number  0
 * k  一天中的小时数（1-24）  Number  24
 * K  am/pm 中的小时数（0-11）  Number  0
 * h  am/pm 中的小时数（1-12）  Number  12
 * m  小时中的分钟数  Number  30
 * s  分钟中的秒数  Number  55
 * S  毫秒数  Number  978
 * z  时区  General time zone  Pacific Standard Time; PST; GMT-08:00
 * Z  时区  RFC 822 time zone  -0800
 *         </pre>
 */
public class TimeUtils extends org.apache.commons.lang.time.DateUtils {


    /**
     * 带有时区的格式为 yyyy.MM.dd G 'at' hh:mm:ss z 如 '2002-1-1 AD at 22:10:59 PSD'
     */
    public final static String FORMAT_FULL_ZONE = "yyyy.MM.dd G 'at' hh:mm:ss z";

    /**
     * 带有时区的格式为yyyy-MM-dd'T'HH:mm:ss.SSSZ 如 '2001-07-04T12:08:56.235-0700'
     */
    public final static String FORMAT_FULL_ZONE_FOR_T = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * 无符号英文简写 如：20101201
     */
    public final static String FORMAT_SHORT_NO_SIGN = "yyyyMMdd";

    /**
     * 斜杠英文简写 如：2010/12/01
     */
    public final static String FORMAT_SHORT_SLASH = "yyyy/MM/dd";

    /**
     * 英文简写（默认）如：2010-12-01
     */
    public final static String FORMAT_SHORT = "yyyy-MM-dd";

    /**
     * CSV和EXCEL默认的时间格式
     */
    public final static String FORMAT_CSV_DATA = "yyyy/MM/dd HH:mm";

    /**
     * 英文简写（默认）如：2010-12
     */
    public final static String FORMAT_YEAR_MONTH = "yyyy-MM";

    public final static String FORMAT_LONG_MIN = "yyyy-MM-dd HH:mm";
    /**
     * 英文全称 如：2010-12-01 23:15:06
     */
    public final static String FORMAT_LONG = "yyyy-MM-dd HH:mm:ss";
    /**
     * 精确到毫秒的完整时间 如：yyyy-MM-dd HH:mm:ss.S
     */
    public final static String FORMAT_FULL = "yyyy-MM-dd HH:mm:ss.S";
    /**
     * 中文简写 如：2010年12月01日
     */
    public final static String FORMAT_SHORT_CN = "yyyy年MM月dd";
    /**
     * 中文全称 如：2010年12月01日 23时15分06秒
     */
    public final static String FORMAT_LONG_CN = "yyyy年MM月dd日  HH时mm分ss秒";
    /**
     * 精确到毫秒的完整中文时间
     */
    public final static String FORMAT_FULL_CN = "yyyy年MM月dd日  HH时mm分ss秒SSS毫秒";

    /**
     * 时间类型
     */
    public final static String[] TIME_TYPES = {"s", "S", "m", "h", "E", "H", "M", "d", "D", "y", "Y"};

    /**
     *
     */
    public final static String[] parsePatterns = {FORMAT_FULL_ZONE_FOR_T, FORMAT_FULL_ZONE, FORMAT_FULL, FORMAT_LONG,
            FORMAT_SHORT, FORMAT_SHORT_NO_SIGN, FORMAT_SHORT_SLASH, FORMAT_CSV_DATA, FORMAT_LONG_MIN};

    /**
     * 格式化当前时间为字符串
     *
     * <pre>
     * 时间：系统当前时间
     * 格式：yyyy-MM-dd HH:mm:ss
     * </pre>
     *
     * @return
     */
    public static String format() {
        return DateFormatUtils.format(millis(), FORMAT_LONG);
    }

    /**
     * 格式化时间为字符串
     *
     * <pre>
     * 格式：yyyy-MM-dd HH:mm:ss
     * </pre>
     *
     * @param date 时间
     * @return
     */
    public static String format(Date date) {
        return DateFormatUtils.format(date, FORMAT_LONG);
    }

    /**
     * 格式化时间为字符串
     *
     * <pre>
     * 格式：yyyy-MM-dd HH:mm:ss
     * </pre>
     *
     * @param date 时间
     * @return
     */
    public static String format(Calendar date) {
        return DateFormatUtils.format(date, FORMAT_LONG);
    }

    /**
     * 格式化时间为字符串
     *
     * <pre>
     * 格式：yyyy-MM-dd HH:mm:ss
     * </pre>
     *
     * @param millis 长整形的时间
     * @return
     */
    public static String format(long millis) {
        return format(new Date(millis), FORMAT_LONG);
    }

    /**
     * 格式化时间为字符串
     *
     * @param date    时间
     * @param pattern 格式
     * @return
     */
    public static String format(Date date, String pattern) {
        return DateFormatUtils.format(date, pattern);
    }

    /**
     * 格式化时间为字符串
     *
     * @param millis  milliseconds时间
     * @param pattern 格式
     * @return
     */
    public static String format(long millis, String pattern) {
        return DateFormatUtils.format(new Date(millis), pattern, null, null);
    }

    /**
     * 格式化时间为字符串
     *
     * @param calendar calendar时间
     * @param pattern  格式
     * @return
     */
    public static String format(Calendar calendar, String pattern) {
        return DateFormatUtils.format(calendar, pattern, null, null);
    }

    /**
     * 获取Timestamp
     *
     * @param date
     * @return
     */
    public static Timestamp toTimestamp(Date date) {
        return new Timestamp(date.getTime());
    }

    /**
     * 获得当前时间的毫秒数
     * <p>
     * 详见{@link System#currentTimeMillis()}
     *
     * @return
     */
    public static long millis() {
        return System.currentTimeMillis();
    }

    /**
     * 获得当前Chinese月份
     *
     * @return
     */
    public static int month(Calendar calendar) {
        return calendar.get(Calendar.MONTH) + 1;
    }

    /**
     * 获得月份中的第几天
     *
     * @return
     */
    public static int dayOfMonth(Calendar calendar) {
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 今天是星期的第几天
     *
     * @return
     */
    public static int dayOfWeek(Calendar calendar) {
        return calendar.get(Calendar.DAY_OF_WEEK);
    }

    /**
     * 今天是年中的第几天
     *
     * @return
     */
    public static int dayOfYear(Calendar calendar) {
        return calendar.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * 判断原日期是否在目标日期之前
     *
     * @param src
     * @param dst
     * @return
     */
    public static boolean isBefore(Date src, Date dst) {
        return src.before(dst);
    }

    /**
     * 判断原日期是否在目标日期之后
     *
     * @param src
     * @param dst
     * @return
     */
    public static boolean isAfter(Date src, Date dst) {
        return src.after(dst);
    }

    /**
     * 判断两日期是否相同
     *
     * @param date1
     * @param date2
     * @return
     */
    public static boolean isEqual(Date date1, Date date2) {
        return date1.compareTo(date2) == 0;
    }

    /**
     * 比较两个时间
     *
     * @param source 源
     * @param target 目标
     * @return 比较值
     */
    public static int compareDate(Date source, Date target) {
        return source.compareTo(target);
    }

    /**
     * 比较两个时间
     *
     * @param source 源
     * @param target 目标
     * @param arr    比较的值是否存在
     * @return 是否是设定的比较值
     */
    public static boolean compareDate(Date source, Date target, int... arr) {
        return ArrayUtils.contains(arr, source.compareTo(target));
    }

    /**
     * 获取现在时间
     *
     * @return
     */
    public static Date today() {
        return jodd.time.TimeUtil.toDate(LocalDate.now());
    }

    /**
     * 明天
     *
     * @return
     */
    public static Date tomorrow() {
        return jodd.time.TimeUtil.toDate(LocalDate.now().plusDays(1));
    }

    /**
     * 昨天
     *
     * @return
     */
    public static Date yesterday() {
        return jodd.time.TimeUtil.toDate(LocalDate.now().plusDays(-1));
    }

    /**
     * 获取现在时间
     *
     * @return
     */
    public static Date now() {
        return jodd.time.TimeUtil.toDate(LocalDateTime.now());
    }

    /**
     * 获取当前时间
     *
     * @return
     */
    public static Calendar calendar() {
        Calendar calendar = GregorianCalendar.getInstance();
        return calendar;
    }

    /**
     * 格式化时间
     *
     * @param dateStr
     * @return
     */
    public static Calendar calendar(String dateStr) {
        return calendar(dateStr, null);
    }

    /**
     * 格式化时间
     *
     * @param dateStr 时间字符串
     * @param reserve 格式化失败返回
     * @return
     */
    public static Calendar calendar(String dateStr, Calendar reserve) {
        Date date = parseDate(dateStr, parsePatterns, null);
        return date == null ? reserve : calendar(date);
    }

    /**
     * 将未知类型转换成为时间格式
     *
     * @param obj
     * @return
     */
    public static Date trueTime(Object obj) {
        return trueTime(obj, null);
    }

    /**
     * 将未知类型转换成为时间格式
     *
     * @param obj
     * @param reserve
     * @return
     */
    public static Date trueTime(Object obj, Date reserve) {
        if (obj == null)
            return reserve;
        if (obj instanceof String)
            return parseDate(obj.toString(), parsePatterns, reserve);
        if (obj instanceof Date)
            return (Date) obj;
        if (obj instanceof Long)
            return new Date((Long) obj);
        if (obj instanceof Calendar)
            return ((Calendar) obj).getTime();
        return reserve;
    }

    /**
     * 将字符串转换为时间
     *
     * @param str
     * @return
     * @throws ParseException
     */
    public static Date parseDate(String str) throws ParseException {
        // DateTime dt = new DateTime(str);
        // return new Date(dt.getMilliseconds(TimeZone.getDefault()));
        return DateUtils.parseDate(str, parsePatterns);
    }

    /**
     * 安全的转换成Date类型，如果不能转换自动返回Null
     *
     * @param str 需要转换的字符串
     * @return
     */
    public static Date softParseDate(String str) {
        try {
            return DateUtils.parseDate(str, parsePatterns);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 安全的转换成Date类型，如果不能转换自动返回Null
     *
     * @param str
     * @param parsePatterns
     * @return
     */
    public static Date softParseDate(String str, String... parsePatterns) {
        try {
            return DateUtils.parseDate(str, parsePatterns);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 安全的转换成Date类型
     *
     * @param str     需要转换的字符串
     * @param reserve 如果转换错误返回相应的Date
     * @return
     */
    public static Date softParseDate(String str, Date reserve) {
        return parseDate(str, reserve);
    }

    /**
     * 转换时间
     *
     * @param str
     * @param parsePattern
     * @return
     * @throws ParseException
     */
    public static Date parseDate(String str, String parsePattern) throws ParseException {
        return DateUtils.parseDate(str, ArrayUtils.newArray(parsePattern));
    }

    /**
     * 将字符串转换为时间
     *
     * @param str     字符串
     * @param reserve 格式化错误备选
     * @return
     */
    public static Date parseDate(String str, Date reserve) {
        return parseDate(str, parsePatterns, reserve);
    }

    /**
     * 将字符串转换为时间
     *
     * @param str          字符串
     * @param parsePattern parsePattern the date format patterns to use, see
     *                     SimpleDateFormat
     * @param reserve      格式化错误备选
     * @return
     */
    public static Date parseDate(String str, String parsePattern, Date reserve) {
        return parseDate(str, ArrayUtils.newArray(parsePattern), reserve);
    }

    /**
     * 将字符串转换为时间
     *
     * @param str           字符串
     * @param parsePatterns parsePatterns the date format patterns to use, see
     *                      SimpleDateFormat
     * @param reserve       格式化错误备选
     * @return
     */
    public static Date parseDate(String str, String[] parsePatterns, Date reserve) {
        try {
            if (StringUtils.isBlank(str))
                return reserve;
            if (ArrayUtils.isEmpty(parsePatterns))
                return reserve;
            return DateUtils.parseDate(str, parsePatterns);
        } catch (ParseException e) {
            return reserve;
        }
    }

    /**
     * 将Date类型转换为日期类型
     *
     * @param date
     * @return
     */
    public static Calendar calendar(Date date) {
        if (date == null)
            return null;
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        // calendar.setFirstDayOfWeek(Calendar.MONDAY);
        return calendar;
    }

    /**
     * 将Date类型转换为日期类型
     *
     * @param date
     * @param firstDayOfWeek 星期开始的一天
     * @return
     */
    public static Calendar calendar(Date date, int firstDayOfWeek) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        calendar.setFirstDayOfWeek(firstDayOfWeek);
        return calendar;
    }

    /**
     * 判断某个日期是否在某个日期范围
     *
     * @param beginDate 日期范围开始
     * @param endDate   日期范围结束
     * @param src       需要判断的日期
     * @return
     */
    public static boolean between(Date beginDate, Date endDate, Date src) {
        return beginDate.before(src) && endDate.after(src);
    }

    /**
     * 获得当前月的第一天
     * <p>
     * HH:mm:ss SS为零
     *
     * @return
     */
    public static Calendar firstDayOfMonth(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH)); // M月置零
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));// H置零
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));// m置零
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));// s置零
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));// S置零
        return firstTimeOfDay(calendar);
    }

    /**
     * 获得当前月的最后一天
     * <p>
     * HH:mm:ss为0，毫秒为999
     *
     * @return
     */
    public static Calendar lastDayOfMonth(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)); // M月置零
        // calendar.set(Calendar.HOUR_OF_DAY,
        // calendar.getActualMaximum(Calendar.HOUR_OF_DAY));// H置零
        // calendar.set(Calendar.MINUTE,
        // calendar.getActualMaximum(Calendar.MINUTE));// m置零
        // calendar.set(Calendar.SECOND,
        // calendar.getActualMaximum(Calendar.SECOND));// s置零
        // calendar.set(Calendar.MILLISECOND,
        // calendar.getActualMaximum(Calendar.MILLISECOND));// S置零
        return lastTimeOfDay(calendar);
    }

    /**
     * 本周的第一天
     *
     * @param calendar
     * @param firstDayOfWeek
     * @return
     */
    public static Calendar firstDayOfWeek(Calendar calendar, Integer firstDayOfWeek) {
        if (firstDayOfWeek != null)
            calendar.setFirstDayOfWeek(firstDayOfWeek);
        int d = calendar.get(Calendar.DAY_OF_WEEK);
        int s = calendar.getFirstDayOfWeek();
        int day_of_week = d > s ? d - s : s - d;
        calendar.add(Calendar.DATE, -day_of_week);
        return firstTimeOfDay(calendar);
    }

    /**
     * 本周的最后一天
     *
     * @param calendar
     * @param firstDayOfWeek
     * @return
     */
    public static Calendar lastDayOfWeek(Calendar calendar, Integer firstDayOfWeek) {
        calendar = firstDayOfWeek(calendar, firstDayOfWeek);
        calendar.add(Calendar.DATE, 6);
        return lastTimeOfDay(calendar);
    }

    /**
     * 本年度的第一天
     *
     * @param calendar
     * @return
     */
    public static Calendar firstDayOfYear(Calendar calendar) {
        calendar.set(Calendar.MONTH, calendar.getActualMinimum(Calendar.MONTH));
        return firstDayOfMonth(calendar);
    }

    /**
     * 本年度的最后一天
     *
     * @param calendar
     * @return
     */
    public static Calendar lastDayOfYear(Calendar calendar) {
        calendar.set(Calendar.MONTH, calendar.getActualMaximum(Calendar.MONTH));
        return lastDayOfMonth(calendar);
    }

    /**
     * 今天的开始时间
     *
     * @param calendar
     * @return
     */
    public static Calendar firstTimeOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        return firstTimeOfHour(calendar);
    }

    /**
     * 今天的结束时间
     *
     * @param calendar
     * @return
     */
    public static Calendar lastTimeOfDay(Calendar calendar) {

        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY));
        return lastTimeOfHour(calendar);
    }

    /**
     * 本小时的开始时间
     *
     * @param calendar
     * @return
     */
    public static Calendar firstTimeOfHour(Calendar calendar) {
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
        return calendar;
    }

    /**
     * 本小时的结束时间
     *
     * @param calendar
     * @return
     */
    public static Calendar lastTimeOfHour(Calendar calendar) {
        calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMaximum(Calendar.MILLISECOND));
        return calendar;
    }

    public static Calendar firstTimeOrDate(Calendar calendar, int field) {
        if (calendar == null)
            return null;
        switch (field) {
            case Calendar.HOUR_OF_DAY:
                return TimeUtils.firstTimeOfHour(calendar);
            case Calendar.DATE:
                return TimeUtils.firstTimeOfDay(calendar);
            case Calendar.WEEK_OF_MONTH:
                return TimeUtils.firstDayOfWeek(calendar, Calendar.MONDAY);
            case Calendar.MONTH:
                return TimeUtils.firstDayOfMonth(calendar);
            case Calendar.YEAR:
                return TimeUtils.firstDayOfYear(calendar);
        }
        return null;
    }

    public static Calendar lastTimeOrDate(Calendar calendar, int field) {
        if (calendar == null)
            return null;
        switch (field) {
            case Calendar.HOUR_OF_DAY:
                return TimeUtils.lastTimeOfHour(calendar);
            case Calendar.DATE:
                return TimeUtils.lastTimeOfDay(calendar);
            case Calendar.WEEK_OF_MONTH:
                return TimeUtils.lastDayOfWeek(calendar, Calendar.MONDAY);
            case Calendar.MONTH:
                return TimeUtils.lastDayOfMonth(calendar);
            case Calendar.YEAR:
                return TimeUtils.lastDayOfYear(calendar);
        }
        return null;
    }

    /**
     * 将String类型转换为long
     *
     * @param inVal
     * @return
     */
    public static long fromDateStringToLong(String inVal) {
        Date date = null;
        SimpleDateFormat inputFormat = new SimpleDateFormat(FORMAT_LONG);
        try {
            date = inputFormat.parse(inVal);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date.getTime();
    }

    /**
     * 将long型转换String
     *
     * @param inVal
     * @return
     */
    public static String fromLongToDate(long inVal) {
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_LONG);
        Date currentTime = new Date(inVal);
        return sdf.format(currentTime);
    }

    /**
     * 将long型转换为Date
     *
     * @param inVal
     * @return
     */
    public static Date fromDateStringToDate(String inVal) {
        Date date = null;
        SimpleDateFormat inputFormat = new SimpleDateFormat(FORMAT_LONG);
        try {
            date = inputFormat.parse(inVal);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * 根据开始时间，结束时间和接收频率算出该时间段应该有多少个时间点，并把每一个时间点的值汇聚成一个list
     *
     * @param startTime
     * @param endTime
     * @param per_hour_frequency
     * @return
     */
    public static List<String> buildPeriodTimeList(String startTime, String endTime, int per_hour_frequency) {

        int countTime = countPeriodTimeDuring1Day(startTime, endTime, per_hour_frequency);
        int addTimeSecond = 3600 / per_hour_frequency; // 根据每小时的频率计算这段时间有多少秒

        List<String> timeList = new ArrayList<String>();

        for (int i = 0, count = countTime + 1; i < count; i++) {
            String time = null;
            if (i == 0) {
                time = startTime;
            } else {
                time = timeList.get(i - 1);
                time = DateFormatUtils.format(DateUtils.addSeconds(TimeUtils.fromDateStringToDate(time), addTimeSecond),
                        TimeUtils.FORMAT_LONG);
            }
            timeList.add(time);
        }
        return timeList;
    }

    /**
     * 计算一天之内该时间段按照统计频率来看应该有多少个点，频率只能精确到秒级 例如： 统计2013-10-16 09:00:00 到2013-10-16
     * 10:00:00 这段时间，如果频率为15分钟，回传4，如果频率为10秒钟，回传360
     *
     * @param startTime          格式：yyyy-mm-dd hh:mm:ss
     * @param endTime            格式：yyyy-mm-dd hh:mm:ss
     * @param per_hour_frequency 每个小时的接收频率
     * @return
     */
    public static int countPeriodTimeDuring1Day(String startTime, String endTime, int per_hour_frequency) {
        int countNumber = 0;
        long startT = TimeUtils.fromDateStringToLong(startTime);
        long endT = TimeUtils.fromDateStringToLong(endTime);

        long mint = (endT - startT) / 1000;

        countNumber = (int) (mint / (3600 / per_hour_frequency));
        return countNumber;
    }

    /**
     * 转换时间类型 </br>
     *
     *
     * <pre>
     * TimtUtils.exchangeTime(["-2", "h"], "m")            = -120
     * TimtUtils.exchangeTime(["-2", "h", "1", "m"],"m")   = -119
     * TimtUtils.exchangeTime(["10", "d", "1", "h"], null) = 14460d
     * TimtUtils.exchangeTime(null, null)                  = 0d
     * </pre>
     *
     * @param array
     * @param toTimeType
     * @return
     */
    public static double exchangeTime(String[] array, String toTimeType) {
        toTimeType = StringUtils.defaultIfBlank(toTimeType, "m");
        array = ArrayUtils.nullToEmpty(array);
        double result = 0d;
        double timeValue = 0d;
        String timeType = "";
        if (array.length % 2 != 0)
            return result;
        for (int i = 1; i < array.length + 1; i++) {
            String object = array[i - 1];
            // 奇偶数判断
            if (i % 2 == 0) {
                timeType = StringUtils.defaultIfBlank(object, "");
                result += exchangeTime(timeValue, timeType, toTimeType);
            } else {
                try {
                    timeValue = Long.parseLong(StringUtils.defaultIfBlank(object, "0"));
                } catch (NumberFormatException e) {
                    return ObjectUtils.LONG_DEF;
                }
            }
        }
        return result;
    }

    /**
     * 转换时间 </br>
     *
     * <pre>
     * TimtUtils.exchangeTime(1l,"h","m")         =60d
     * TimtUtils.exchangeTime(1l,"sdafa","sdfad") =0d
     * TimtUtils.exchangeTime(1l,"sdafa",null)    =0d
     * </pre>
     *
     * @param timeValue  原始时间
     * @param timeType   原始时间类型
     * @param toTimeType 需要转换的时间类型
     * @return 转换完成的时间类型
     */
    public static double exchangeTime(double timeValue, String timeType, String toTimeType) {
        if (!ArrayUtils.contains(TIME_TYPES, timeType))
            return ObjectUtils.LONG_DEF;
        if (!ArrayUtils.contains(TIME_TYPES, toTimeType))
            return ObjectUtils.LONG_DEF;
        if (timeType == toTimeType)
            return timeValue;
        double one = getExchangeRate(timeType);
        double two = getExchangeRate(toTimeType);
        if (one > two) {
            double rate = two == 0d ? one : one / two;
            return timeValue * (rate);
        } else {
            double rate = one == 0d ? two : two / one;
            return timeValue / (rate);
        }
    }

    private static double getExchangeRate(String timeType) {
        double exchangeRate = 0d;
        if (StringUtils.equals(timeType, "S"))
            exchangeRate = 1d;
        if (StringUtils.equals(timeType, "s"))
            exchangeRate = 1000d;
        if (StringUtils.equals(timeType, "m"))
            exchangeRate = 1000d * 60;
        if (StringUtils.equalsIgnoreCase(timeType, "h"))
            exchangeRate = 1000d * 60 * 60;
        if (StringUtils.equalsIgnoreCase(timeType, "d"))
            exchangeRate = 1000d * 60 * 60 * 24;
        if (StringUtils.equals(timeType, "E"))
            exchangeRate = 1000d * 60 * 60 * 24 * 7;
        // 固定一个月为30天(作为日期计算存在误差)
        if (StringUtils.equals(timeType, "M"))
            exchangeRate = 1000d * 60 * 60 * 24 * 30;
        // 固定一年为365天(作为日期计算存在误差)
        if (StringUtils.equalsIgnoreCase(timeType, "y"))
            exchangeRate = 1000d * 60 * 60 * 24 * 365;
        return exchangeRate;
    }

    /**
     * 获取指定日期的月份最后一天
     *
     * @param date
     * @return
     */
    public static Date getEndDayForMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DATE, 1);
        cal.roll(Calendar.DATE, -1);
        return cal.getTime();
    }

    /**
     * 两个时间之间相差距离多少时间，unit参数设置返回的时间单位
     *
     * <pre>
     * getTimeDistance(TimeUnit.HOURS, "2014-11-18","2014-11-19")        = -24
     * getTimeDistance(TimeUnit.DAYS, "2014-11-18","2014-11-19")         = -1
     * getTimeDistance(TimeUnit.MILLISECONDS, "2014-11-18","2014-11-19") = -1440
     * </pre>
     *
     * @param unit 相差的时间类型
     * @param one  时间参数 1：
     * @param two  时间参数 2：
     * @return 相差多久
     */
    public static long compareDate(Date one, Date two, TimeUnit unit) {
        long diff = one.getTime() - two.getTime();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    /**
     * 两个时间之间相差距离多少时间，unit参数设置返回的时间单位
     *
     * <pre>
     * getTimeDistance(TimeUnit.HOURS, 90000000l)        = 25
     * getTimeDistance(TimeUnit.DAYS, 90000000l)         = 1500
     * getTimeDistance(TimeUnit.MILLISECONDS, 90000000l) = 1
     * </pre>
     *
     * @param unit 相差的时间类型
     * @param diff 相差时间
     * @return 相差多久
     */
    public static long getTimeDistance(TimeUnit unit, Long diff) {
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    /**
     * 通过天切割日期
     *
     * @param beginTime
     * @param endTime
     * @return
     */
    public static List<Date> splitTimeForDay(Date beginTime, Date endTime) {
        return splitTime(beginTime, endTime, Calendar.DAY_OF_MONTH, 1);
    }

    /**
     * 通过时间类型切割日期
     *
     * @param beginTime
     * @param endTime
     * @param field
     * @param amount
     * @return
     */
    public static List<Date> splitTime(Date beginTime, Date endTime, int field, int amount) {
        if (beginTime.after(endTime))
            return new ArrayList<Date>();
        // Long dayLen = getTimeDistance(TimeUnit.DAYS, beginTime, endTime);
        List<Date> days = new ArrayList<Date>();
        Calendar b = calendar(beginTime);
        Calendar e = calendar(endTime);
        while (b.getTimeInMillis() <= e.getTimeInMillis()) {
            days.add(b.getTime());
            b.add(field, amount);
        }
        return days;
    }

    /**
     * 时间单位转换
     *
     * @param source
     * @param sourceUnit
     * @param targetUnit
     */
    public static long timeTo(long source, TimeUnit sourceUnit, TimeUnit targetUnit) {
        switch (targetUnit) {
            case NANOSECONDS:
                return sourceUnit.toNanos(source);
            case MICROSECONDS:
                return sourceUnit.toMicros(source);
            case MILLISECONDS:
                return sourceUnit.toMillis(source);
            case SECONDS:
                return sourceUnit.toSeconds(source);
            case MINUTES:
                return sourceUnit.toMinutes(source);
            case HOURS:
                return sourceUnit.toHours(source);
            case DAYS:
                return sourceUnit.toDays(source);
            default:
                return source;
        }
    }

    /**
     * 查找时间段内的星期几
     *
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @param weekDay   星期几，1-7，星期一开始
     * @return
     */
    public static List<Date> findWeekDay(Date beginTime, Date endTime, int weekDay) {
        List<Date> lists = new ArrayList<>();
        Integer dayOfWeek = TimeUtils.dayOfWeek(TimeUtils.calendar(endTime));
        weekDay = weekDay + 1;
        if (weekDay > 7)
            weekDay = 1;
        int offset = dayOfWeek - weekDay;
        if (weekDay > dayOfWeek) {
            offset = 0 - dayOfWeek - (7 - weekDay);
        }
        Date nTime = TimeUtils.addDays(endTime, offset * -1);
        while (nTime.getTime() >= beginTime.getTime()) {
            lists.add(0, nTime);
            nTime = TimeUtils.addDays(nTime, -7);
        }
        return lists;
    }

    /**
     * 统计运行时间
     *
     * @param beginDate
     * @param endDate
     * @return
     */
    public static String countRunTime(Date beginDate, Date endDate) {
        String[] titles = new String[]{"%s天", "%s小时", "%s分", "%s秒", "%s毫秒"};
        long between = DateUtil.betweenMs(beginDate, endDate);
//        long week = between / (7 * 24 * 60 * 60 * 1000);
        long day = between / (24 * 60 * 60 * 1000);
        long hour = (between / (60 * 60 * 1000) - day * 24);
        long min = ((between / (60 * 1000)) - day * 24 * 60 - hour * 60);
        long s = (between / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);
        long ms = (between - day * 24 * 60 * 60 * 1000 - hour * 60 * 60 * 1000
                - min * 60 * 1000 - s * 1000);
        long[] values = new long[]{day, hour, min, s, ms};
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < titles.length; i++) {
            String title = titles[i];
            long val = values[i];
            if (val <= 0)
                continue;
            sb.append(String.format(title, val));
        }
        return sb.toString();
    }

    /**
     * @param args
     * @throws ParseException
     */
    public static void main(String[] args) throws ParseException {
        String date = "2001-07-04T12:08:56.235-0700";
        // // String cats[] = new String[] { FORMAT_LONG };
        Calendar calendar = calendar(date);
        // calendar.clear(Calendar.DAY_OF_WEEK);
        // // calendar.setFirstDayOfWeek(Calendar.SUNDAY);
        //
        // Calendar changeDate = calendar;

        System.out.println("比较时间" + TimeUtils.compareDate(TimeUtils.today(), TimeUtils.tomorrow()));
        System.out.println("比较时间" + TimeUtils.compareDate(TimeUtils.tomorrow(), TimeUtils.today()));
        System.out.println("比较时间" + TimeUtils.compareDate(TimeUtils.today(), TimeUtils.today()));

        System.out.println("比较时间" + TimeUtils.compareDate(TimeUtils.today(), TimeUtils.tomorrow(), -1));
        System.out.println("比较时间" + TimeUtils.compareDate(TimeUtils.tomorrow(), TimeUtils.today(), -1));
        System.out.println("比较时间" + TimeUtils.compareDate(TimeUtils.today(), TimeUtils.today(), -1));

        System.out.println("现在时间" + TimeUtils.format(calendar(), FORMAT_FULL_CN));
        System.out.println("转换时间" + TimeUtils.format(calendar, FORMAT_FULL_CN));
        System.out.println("---------");
        System.out.println("当天开始时间" + DateFormatUtils.format(firstTimeOfDay(calendar), FORMAT_LONG));
        System.out.println("当天结束时间" + DateFormatUtils.format(lastTimeOfDay(calendar), FORMAT_LONG));
        System.out.println("---------");
        System.out.println("本周第一天" + DateFormatUtils.format(firstDayOfWeek(calendar, Calendar.MONDAY), FORMAT_LONG));
        System.out.println("本周最后天" + DateFormatUtils.format(lastDayOfWeek(calendar, Calendar.MONDAY), FORMAT_LONG));
        System.out.println("---------");
        System.out.println("当月开始时间" + DateFormatUtils.format(firstDayOfMonth(calendar), FORMAT_LONG));
        System.out.println("当月结束时间" + DateFormatUtils.format(lastDayOfMonth(calendar), FORMAT_LONG));
        System.out.println("---------");
        System.out.println("本年第一天" + DateFormatUtils.format(firstDayOfYear(calendar), FORMAT_LONG));
        System.out.println("本年最后天" + DateFormatUtils.format(lastDayOfYear(calendar), FORMAT_LONG));

        // String dateTime =
        // MessageFormat.format("{0,date,yyyy-MM-dd-HH-mm:ss:ms}", new Object[]
        // { new java.sql.Date(System.currentTimeMillis()) });
        // System.out.println(dateTime);
    }
}
