package test.util;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Console;
import com.jbm.util.TimeWindowUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TimeWindowUtilsTest {

    private Date currentTime;

    @Before
    public void begin() {
        currentTime = DateTime.now();
    }

    @Test
    public void testGetTimeWindowStart() {
        Console.log(TimeWindowUtils.getTimeWindowStart(currentTime, 15));
    }

    @Test
    public void testGetTimeWindowEnd() {
        Console.log(TimeWindowUtils.getTimeWindowStart(currentTime, 15));
    }

    @Test
    public void testGetTimeWindow() {
        Console.log(TimeWindowUtils.getTimeWindow(currentTime, 15));
    }

    @Test
    public void testGetTimeWindow2() {
        Console.log("hour:{}",TimeWindowUtils.getTimeWindow(currentTime, DateField.HOUR,1));
        Console.log("day:{}",TimeWindowUtils.getTimeWindow(currentTime, DateField.DAY_OF_MONTH,1));
        Console.log("year:{}",TimeWindowUtils.getTimeWindow(currentTime, DateField.YEAR,1));

    }

}