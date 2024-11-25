package com.jbm.test;

import cn.hutool.core.date.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.data.influx.InfluxDateUtil;

import java.util.Date;

public class InfluxDateUtilTest {

    @Test
    public void testDateUtil() {
        Date now = DateTime.now();
        System.out.println(now);
        String utcNow = InfluxDateUtil.toUtcTimeStr(now);
        System.out.println(utcNow);
        System.out.println(InfluxDateUtil.formUtcToDate(utcNow));
    }
}
