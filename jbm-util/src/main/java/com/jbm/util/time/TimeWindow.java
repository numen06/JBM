package com.jbm.util.time;

import lombok.Data;

import java.time.LocalTime;
import java.util.Date;

@Data
public class TimeWindow {

    private LocalTime startTime;
    private LocalTime endTime;

    public TimeWindow() {}

    public TimeWindow(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
