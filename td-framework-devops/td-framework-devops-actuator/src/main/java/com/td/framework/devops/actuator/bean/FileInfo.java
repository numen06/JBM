package com.td.framework.devops.actuator.bean;

import java.io.Serializable;
import java.util.List;

import com.td.util.ListUtils;

/**
 * 
 * @author Leonid
 *
 */
public class FileInfo implements Serializable {

	private static final long serialVersionUID = -960311350433880407L;

	private Long id;

	private String name;

	private long size;

	private String path;

	private boolean isFolder;

	private long updateTime;

	private boolean isRootFolder;
	
	private boolean isEdit;

	private List<FileInfo> children = ListUtils.newArrayList();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isFolder() {
		return isFolder;
	}

	public void setFolder(boolean isFolder) {
		this.isFolder = isFolder;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

	public List<FileInfo> getChildren() {
		return children;
	}

	public void setChildren(List<FileInfo> children) {
		this.children = children;
	}

	public boolean isRootFolder() {
		return isRootFolder;
	}

	public void setRootFolder(boolean isRootFolder) {
		this.isRootFolder = isRootFolder;
	}

	public boolean getIsParent() {
		return this.isFolder;
	}

	public boolean getOpen() {
		return this.isFolder;
	}

	public boolean isEdit() {
		return isEdit;
	}

	public void setEdit(boolean isEdit) {
		this.isEdit = isEdit;
	}
	
}
