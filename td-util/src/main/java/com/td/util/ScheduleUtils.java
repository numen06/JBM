package com.td.util;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author wesley.zhang
 * @date 2017年11月7日
 * @version 1.0
 *
 */
public class ScheduleUtils {

	/**
	 * 循环执行一个方法
	 * 
	 * @param task
	 * @param delay
	 * @param period
	 */
	public static void executeAtFixedRate(TimerTask task, long delay, long period) {
		Timer timer = new Timer();
		timer.schedule(task, delay, period);
	}
}
