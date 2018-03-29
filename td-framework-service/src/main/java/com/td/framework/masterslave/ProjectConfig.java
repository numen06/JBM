package com.td.framework.masterslave;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;

import com.td.framework.metadata.exceptions.PauseException;
import com.td.framework.tools.AbstactConfigurationProperties;

/**
 * 项目的配置
 * 
 * @author Wesley
 * 
 */
public class ProjectConfig extends AbstactConfigurationProperties {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String projectName = "undefined";
	private String projectKey = "undefined";
	private String projectId = "0";
	private int projectInterval = 4;
	private long projectSequence = 0l;
	private Boolean masterSlave = false;

	public Boolean getMasterSlave() {
		return masterSlave;
	}

	public void setMasterSlave(Boolean masterSlave) {
		this.masterSlave = masterSlave;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public long getProjectSequence() {
		return projectSequence;
	}

	public int getProjectInterval() {
		return projectInterval;
	}

	public void setProjectInterval(int projectInterval) {
		this.projectInterval = projectInterval;
	}

	public void initPropertys() {
		try {
			this.fillPropertys(super.getConfigurationProperties());
			this.projectSequence = initProjectSequence(this.getProjectId());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		if (this.projectSequence != 0 && projectInterval != projectId.length()) {
			throw new PauseException("当前设置的ProjectId和ProjectInterval的长度不匹配");
		}
	}

	protected static long initProjectSequence(String projectId) {
		projectId = StringUtils.trimToEmpty(projectId);
		StringBuffer sb = new StringBuffer(Long.MAX_VALUE + "");
		int size = sb.length();
		char[] ll = new char[size];
		Arrays.fill(ll, '0');
		String ss = new String(ll);
		sb = new StringBuffer(ss);
		sb.insert(0, projectId);
		return Long.parseLong(sb.substring(0, size - 1));
	}

	public static void main(String[] args) {
		System.out.println(initProjectSequence(null));
	}
}
