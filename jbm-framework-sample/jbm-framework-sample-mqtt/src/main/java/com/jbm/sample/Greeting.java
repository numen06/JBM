package com.jbm.sample;

import java.io.Serializable;

public class Greeting implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String content;

	public Greeting(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}

}