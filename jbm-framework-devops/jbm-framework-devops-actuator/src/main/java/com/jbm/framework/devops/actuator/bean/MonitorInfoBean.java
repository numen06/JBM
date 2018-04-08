package com.jbm.framework.devops.actuator.bean;

import java.util.Date;

public class MonitorInfoBean {
	/**
	 * 总内存
	 */
	private long totalMemory;
	/**
	 * 系统名称
	 */
	private String osName;
	/**
	 * 使用内存
	 */
	private long usedMemory;
	private Double cpuRatio;
	private Double memRatio;
	private Double diskRatio;
	private long totalDisk;
	private long freeDisk;
	private Date monitorTime;

	public long getTotalMemory() {
		return totalMemory;
	}

	public void setTotalMemory(long totalMemory) {
		this.totalMemory = totalMemory;
	}

	public String getOsName() {
		return osName;
	}

	public void setOsName(String osName) {
		this.osName = osName;
	}

	public long getUsedMemory() {
		return usedMemory;
	}

	public void setUsedMemory(long usedMemory) {
		this.usedMemory = usedMemory;
	}

	public Double getCpuRatio() {
		return cpuRatio;
	}

	public void setCpuRatio(Double cpuRatio) {
		this.cpuRatio = cpuRatio;
	}

	public Double getMemRatio() {
		return memRatio;
	}

	public void setMemRatio(Double memRatio) {
		this.memRatio = memRatio;
	}

	public Double getDiskRatio() {
		return diskRatio;
	}

	public void setDiskRatio(Double diskRatio) {
		this.diskRatio = diskRatio;
	}

	public long getTotalDisk() {
		return totalDisk;
	}

	public void setTotalDisk(long totalDisk) {
		this.totalDisk = totalDisk;
	}

	public long getFreeDisk() {
		return freeDisk;
	}

	public void setFreeDisk(long freeDisk) {
		this.freeDisk = freeDisk;
	}

	public Date getMonitorTime() {
		return monitorTime;
	}

	public void setMonitorTime(Date monitorTime) {
		this.monitorTime = monitorTime;
	}

}
