package test.util;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Console;
import cn.hutool.log.dialect.console.ConsoleLog;
import com.jbm.util.TimeWindowUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

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
}