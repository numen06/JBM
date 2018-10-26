/**
 * 
 */
package com.jbm.framework.devops.actuator.schedule;

import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jbm.util.ListUtils;

/**
 * @author Leonid
 *
 */
public class ScheduleTaskPool {

	private static Logger logger = LoggerFactory.getLogger(ScheduleTaskPool.class);

	private static ConcurrentHashMap<String, ProcessMonitorSchedule> scheduleCache = new ConcurrentHashMap<String, ProcessMonitorSchedule>();
	// static Cache<String, ProcessMonitorSchedule> scheduleCache =
	// CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();

	/**
	 * 
	 */
	private ScheduleTaskPool() {
	}

	public static void put(String key, ProcessMonitorSchedule val) {
		final String kk = exchageKey(key);
		if (scheduleCache.containsKey(kk)) {
			logger.info("[put] containskey {} - val {}", kk, val);
			return;
		}
		logger.info("[put] key {} - val {}", kk, val);
		scheduleCache.put(key, val);
	}

	public static void update(String key, ProcessMonitorSchedule val) {
		final String kk = exchageKey(key);
		logger.info("[update] key {} - val {}", kk, val);
		remove(key);
		scheduleCache.put(kk, val);
	}

	public static ProcessMonitorSchedule get(String key) {
		final String kk = exchageKey(key);
		// ProcessMonitorSchedule service = scheduleCache.get(key);
		// if (service != null) {
		// if (!service.isRunning()) {
		// scheduleCache.remove(key);
		// }
		// }
		return scheduleCache.get(kk);
	}

	public static boolean remove(String key) {
		final String kk = exchageKey(key);
		ProcessMonitorSchedule service = scheduleCache.get(kk);
		if (service != null) {
			if (service.isRunning()) {
				service.stopAsync();
			}
		}
		logger.info("[remove] key {}", kk);
		return scheduleCache.remove(kk, service);
	}

	public static Enumeration<String> keySet() {
		return scheduleCache.keys();
	}

	public static List<String> keyList() {
		List<String> keyList = ListUtils.newArrayList();
		Enumeration<String> keys = scheduleCache.keys();
		String key = null;
		while (keys.hasMoreElements()) {
			key = keys.nextElement();
			keyList.add(key);
		}
		return keyList;
	}

	public static String exchageKey(String key) {
		return StringUtils.replace(key, "\\", "/");
	}

	public static boolean exist(String key) {
		final String kk = exchageKey(key);
		return scheduleCache.containsKey(kk);
	}
}
