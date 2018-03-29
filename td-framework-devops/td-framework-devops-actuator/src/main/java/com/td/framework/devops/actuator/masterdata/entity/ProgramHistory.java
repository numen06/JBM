package com.td.framework.devops.actuator.masterdata.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.td.masterdata.entity.common.MasterEntity;

@Entity
@Table(name = "program_history")
public class ProgramHistory extends MasterEntity<String> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 备份时间
	 */
	private Date backupTime;
	/**
	 * 主文件
	 */
	private String mianFile;
	/**
	 * 文件夹位置
	 */
	private String folder;

	public Date getBackupTime() {
		return backupTime;
	}

	public void setBackupTime(Date backupTime) {
		this.backupTime = backupTime;
	}

	public String getMianFile() {
		return mianFile;
	}

	public void setMianFile(String mianFile) {
		this.mianFile = mianFile;
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

}
