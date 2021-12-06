package com.jbm.util.time;

import cn.hutool.core.date.DateTime;
import com.jbm.util.TimeUtils;

import java.util.Date;

/**
 * 运行时间跟踪
 */
public class RuntimeTrack {

    private Date startTime = DateTime.now();

    private Date endTime;


    public void string() {
        startTime = DateTime.now();
    }

    public void end() {
        endTime = DateTime.now();
    }

    @Override
    public String toString() {
        return TimeUtils.countRunTime(startTime, endTime);
    }

    public void print() {
        System.out.println("运行了:" + this.toString());
    }
}
