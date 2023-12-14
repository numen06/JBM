package com.jbm.util.oshi.info;

import lombok.Data;

/**
 * 监视信息的JavaBean类.
 * 
 * @author amg
 * @version 1.0 Creation date: 2008-4-25 - 上午10:37:00
 */
@Data
public class MonitorInfo {

	/** 总的物理内存. */
	private String totalMemorySize;

	/** 已使用的物理内存. */
	private String usedMemory;

	/** cpu使用率. */
	private String cpuRatio;
	
	/** 内存使用率. */
	private String memRatio;

	/**
	 * cpu数量
	 */
	private Integer cpuCount;

	/**
	 * 线程数量
	 */
	private Integer threadCount;

}