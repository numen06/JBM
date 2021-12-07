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


    public void start() {
        this.startTime = DateTime.now();
    }

    public void start(final Date startTime) {
        this.startTime = startTime;
    }


    public void end() {
        this.endTime = DateTime.now();
    }

    public void end(final Date endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return TimeUtils.countRunTime(startTime, endTime);
    }

    public void print() {
        System.out.println("运行了:" + this);
    }
}
