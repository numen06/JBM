package test.util;

import cn.hutool.core.thread.ThreadUtil;
import com.jbm.util.TimeUtils;
import com.jbm.util.time.RuntimeTrack;
import junit.framework.TestCase;
import org.apache.commons.lang.time.DateFormatUtils;
import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class TimeUtilTest extends TestCase {

    public void testZone() {
        // create default time zone object
        TimeZone timezonedefault = TimeZone.getDefault();
        // checking default time zone value
        System.out.println("Default time zone is :\n" + timezonedefault);
    }

    @Test
    public void testParseDate() throws ParseException {
        System.out.println(TimeUtils.parseDate("2016/04/05"));
        // System.out.println(TimeUtil.parseDate("2016年04月05日 00时47分59秒511毫秒"));
    }

    @Test
    public void testGetBeforeMin() {
        Date result = TimeUtils.addMinutes(TimeUtils.now(), -10);
        System.out.println(TimeUtils.format(result));
    }

    @Test
    public void testIsAfter() {
        Date now = TimeUtils.now();
        boolean result = TimeUtils.isBefore(TimeUtils.addMinutes(now, -21), TimeUtils.softParseDate("2015-04-23 15:24:00"));
        System.out.println(result);
    }

    @Test
    public void testNow() {
        System.out.print("now:");
        Date result = TimeUtils.now();
        System.out.println(TimeUtils.format(result));
    }

    @Test
    public void testToday() {
        System.out.print("today:");
        Date result = TimeUtils.today();
        System.out.println(TimeUtils.format(result));
    }

    @Test
    public void testTomorrow() {
        System.out.print("tomorrow:");
        Date result = TimeUtils.tomorrow();
        System.out.println(TimeUtils.format(result));
    }

    @Test
    public void testYesterday() {
        System.out.print("yesterday:");
        Date result = TimeUtils.yesterday();
        System.out.println(TimeUtils.format(result));
    }

    @Test
    public void getTimeDistance() {
        System.out.println(TimeUtils.getTimeDistance(TimeUnit.HOURS, 90000000l));
        System.out.println(TimeUtils.getTimeDistance(TimeUnit.MINUTES, 90000000l));
        System.out.println(TimeUtils.getTimeDistance(TimeUnit.DAYS, 90000000l));
    }

    @Test
    public void testSplitTimeForDay() {
        System.out.println(TimeUtils.splitTimeForDay(TimeUtils.yesterday(), TimeUtils.tomorrow()));
    }

    @Test
    public void testSplitTime() throws ParseException {
        System.out.println(TimeUtils.splitTime(TimeUtils.parseDate("2015-06-04 06:00:00"), TimeUtils.parseDate("2015-06-04 22:00:00"), Calendar.MINUTE, 15));
    }

    @Test
    public void testFindWeekDay() throws ParseException {
        List<Date> days = TimeUtils.findWeekDay(TimeUtils.parseDate("2017-05-01 06:00:00"), TimeUtils.parseDate("2017-05-31 22:00:00"), 7);
        for (Date date : days) {
            System.out.println(TimeUtils.format(date));
        }
    }

    @Test
    public void testDif() {
        RuntimeTrack runtimeTrack = new RuntimeTrack();
        ThreadUtil.safeSleep(1000);
        runtimeTrack.end();
        runtimeTrack.print();
    }

    @Test
    public void testAll() {
        String date = "2001-07-04T12:08:56.235-0700";
        // // String cats[] = new String[] { FORMAT_LONG };
        Calendar calendar = TimeUtils.calendar(date);
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

        System.out.println("现在时间" + TimeUtils.format(TimeUtils.calendar(), TimeUtils.FORMAT_FULL_CN));
        System.out.println("转换时间" + TimeUtils.format(calendar, TimeUtils.FORMAT_FULL_CN));
        System.out.println("---------");
        System.out.println("当天开始时间" + DateFormatUtils.format(TimeUtils.firstTimeOfDay(calendar), TimeUtils.FORMAT_LONG));
        System.out.println("当天结束时间" + DateFormatUtils.format(TimeUtils.lastTimeOfDay(calendar), TimeUtils.FORMAT_LONG));
        System.out.println("---------");
        System.out.println("本周第一天" + DateFormatUtils.format(TimeUtils.firstDayOfWeek(calendar, Calendar.MONDAY), TimeUtils.FORMAT_LONG));
        System.out.println("本周最后天" + DateFormatUtils.format(TimeUtils.lastDayOfWeek(calendar, Calendar.MONDAY), TimeUtils.FORMAT_LONG));
        System.out.println("---------");
        System.out.println("当月开始时间" + DateFormatUtils.format(TimeUtils.firstDayOfMonth(calendar), TimeUtils.FORMAT_LONG));
        System.out.println("当月结束时间" + DateFormatUtils.format(TimeUtils.lastDayOfMonth(calendar), TimeUtils.FORMAT_LONG));
        System.out.println("---------");
        System.out.println("本年第一天" + DateFormatUtils.format(TimeUtils.firstDayOfYear(calendar), TimeUtils.FORMAT_LONG));
        System.out.println("本年最后天" + DateFormatUtils.format(TimeUtils.lastDayOfYear(calendar), TimeUtils.FORMAT_LONG));

        // String dateTime =
        // MessageFormat.format("{0,date,yyyy-MM-dd-HH-mm:ss:ms}", new Object[]
        // { new java.sql.Date(System.currentTimeMillis()) });
        // System.out.println(dateTime);
    }
}
