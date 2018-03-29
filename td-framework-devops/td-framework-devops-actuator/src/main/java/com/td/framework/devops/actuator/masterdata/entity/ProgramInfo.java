package com.td.framework.devops.actuator.masterdata.entity;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.td.framework.devops.actuator.bean.BaseInfo;
import com.td.util.TimeUtil;

@Entity
@Table(name = "program_info")
public class ProgramInfo extends BaseInfo {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1500913159866771376L;

	/**
	 * 开始时间
	 */
	private Date startTime;
	/**
	 * 结束时间
	 */
	private Date endTime;
	/**
	 * 激活时间
	 */
	private Date activationTime;

	private Boolean guard = true;

	private Integer mport;

	public Boolean getGuard() {
		return guard;
	}

	public void setGuard(Boolean guard) {
		this.guard = guard;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Long getRunTime() {
		if (startTime == null || this.getPid() == null)
			return null;
		return TimeUtil.getTimeDistance(TimeUnit.MINUTES, new Date(), startTime);
	}

	public Long getStandUpTime() {
		if (startTime == null || activationTime == null)
			return null;
		return TimeUtil.getTimeDistance(TimeUnit.SECONDS, activationTime, startTime);
	}

	public Integer getMport() {
		return mport;
	}

	public void setMport(Integer mport) {
		this.mport = mport;
	}

	public Date getActivationTime() {
		return activationTime;
	}

	public void setActivationTime(Date activationTime) {
		this.activationTime = activationTime;
	}

}
