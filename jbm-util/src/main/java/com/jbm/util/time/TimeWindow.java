package com.jbm.util.time;

import cn.hutool.core.date.DateTime;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Date;

/**
 * @author wesley
 */
@Data
public class TimeWindow implements Serializable {

    private Date startTime;
    private Date endTime;

    public TimeWindow() {}

    public TimeWindow(Date startTime, Date endTime) {
        this.startTime = DateTime.of(startTime);
        this.endTime = DateTime.of(endTime);
    }
}
