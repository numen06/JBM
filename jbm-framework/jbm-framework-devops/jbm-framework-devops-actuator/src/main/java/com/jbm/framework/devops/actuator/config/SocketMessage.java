package com.jbm.framework.devops.actuator.config;

import java.io.Serializable;
import java.util.Date;

public class SocketMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8362647287611616657L;

	public String message;

	public Date date;

	public int type;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

}
