package com.td.devops.bean;

import java.io.Serializable;
import java.util.List;

import com.td.framework.metadata.usage.bean.FileInfoBean;

public class ReleaseOption implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Boolean start;

	private Boolean release;

	private List<FileInfoBean> fileInfos;

	private List<String> urls;

	private String dirname;

	public Boolean getStart() {
		return start;
	}

	public void setStart(Boolean start) {
		this.start = start;
	}

	public Boolean getRelease() {
		return release;
	}

	public void setRelease(Boolean release) {
		this.release = release;
	}

	public List<FileInfoBean> getFileInfos() {
		return fileInfos;
	}

	public void setFileInfos(List<FileInfoBean> fileInfos) {
		this.fileInfos = fileInfos;
	}

	public List<String> getUrls() {
		return urls;
	}

	public void setUrls(List<String> urls) {
		this.urls = urls;
	}

	public String getDirname() {
		return dirname;
	}

	public void setDirname(String dirname) {
		this.dirname = dirname;
	}

}
