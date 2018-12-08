/**
 * 
 */
package com.jbm.framework.devops.actuator.bean;

import java.util.Date;

/**
 * @author Leonid
 *
 */
public class RecoveryInfo extends BaseInfo {

	private static final long serialVersionUID = 1477543448614070735L;

	/**
	 * 
	 */
	public RecoveryInfo() {
		// TODO Auto-generated constructor stub
	}
	
	private Date recoveryDate;
	
	private String sourcePath;
	
	private String sourceName;
	
	private String type;
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Date getRecoveryDate() {
		return recoveryDate;
	}

	public void setRecoveryDate(Date recoveryDate) {
		this.recoveryDate = recoveryDate;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}
	

}
