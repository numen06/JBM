package test.util;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.td.util.TimeUtil;

import junit.framework.TestCase;

public class TimeUtilTest extends TestCase {

	public void testZone() {
		// create default time zone object
		TimeZone timezonedefault = TimeZone.getDefault();
		// checking default time zone value
		System.out.println("Default time zone is :\n" + timezonedefault);
	}

	@Test
	public void testParseDate() throws ParseException {
		System.out.println(TimeUtil.parseDate("2016/04/05"));
		// System.out.println(TimeUtil.parseDate("2016年04月05日 00时47分59秒511毫秒"));
	}

	@Test
	public void testGetBeforeMin() {
		Date result = TimeUtil.getBeforeMin(TimeUtil.now(), -10);
		System.out.println(TimeUtil.format(result));
	}

	@Test
	public void testIsAfter() {
		Date now = TimeUtil.now();
		boolean result = TimeUtil.isBefore(TimeUtil.getBeforeMin(now, -21), TimeUtil.softParseDate("2015-04-23 15:24:00"));
		System.out.println(result);
	}

	@Test
	public void testNow() {
		System.out.print("now:");
		Date result = TimeUtil.now();
		System.out.println(TimeUtil.format(result));
	}

	@Test
	public void testToday() {
		System.out.print("today:");
		Date result = TimeUtil.today();
		System.out.println(TimeUtil.format(result));
	}

	@Test
	public void testTomorrow() {
		System.out.print("tomorrow:");
		Date result = TimeUtil.tomorrow();
		System.out.println(TimeUtil.format(result));
	}

	@Test
	public void testYesterday() {
		System.out.print("yesterday:");
		Date result = TimeUtil.yesterday();
		System.out.println(TimeUtil.format(result));
	}

	@Test
	public void getTimeDistance() {
		System.out.println(TimeUtil.getTimeDistance(TimeUnit.HOURS, 90000000l));
		System.out.println(TimeUtil.getTimeDistance(TimeUnit.MINUTES, 90000000l));
		System.out.println(TimeUtil.getTimeDistance(TimeUnit.DAYS, 90000000l));
	}

	@Test
	public void testSplitTimeForDay() {
		System.out.println(TimeUtil.splitTimeForDay(TimeUtil.yesterday(), TimeUtil.tomorrow()));
	}

	@Test
	public void testSplitTime() throws ParseException {
		System.out.println(TimeUtil.splitTime(TimeUtil.parseDate("2015-06-04 06:00:00"), TimeUtil.parseDate("2015-06-04 22:00:00"), Calendar.MINUTE, 15));
	}

	@Test
	public void testFindWeekDay() throws ParseException {
		List<Date> days = TimeUtil.findWeekDay(TimeUtil.parseDate("2017-05-01 06:00:00"), TimeUtil.parseDate("2017-05-31 22:00:00"),7);
		for (Date date : days) {
			System.out.println(TimeUtil.format(date));
		}
	}
}
