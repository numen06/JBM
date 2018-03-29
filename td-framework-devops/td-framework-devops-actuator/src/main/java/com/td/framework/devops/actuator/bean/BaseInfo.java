package com.td.framework.devops.actuator.bean;

import java.util.Date;

import javax.persistence.MappedSuperclass;

import com.td.masterdata.entity.common.MasterEntity;

import jodd.io.FileNameUtil;

/**
 * @author wesley.zhang
 *
 */
@MappedSuperclass
public class BaseInfo extends MasterEntity<String> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer pid;
	private String mianFile;
	private String folder;
	private Boolean stauts;
	private Date createTime;

	public Integer getPid() {
		return pid;
	}

	public void setPid(Integer pid) {
		this.pid = pid;
	}

	public String getMianFile() {
		return mianFile;
	}

	public void setMianFile(String mianFile) {
		this.mianFile = FileNameUtil.normalize(mianFile, true);
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = FileNameUtil.normalize(folder, true);
	}

	public Boolean getStauts() {
		return stauts;
	}

	public void setStauts(Boolean stauts) {
		this.stauts = stauts;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

}
