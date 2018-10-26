package com.jbm.framework.devops.env.bean;

import org.kohsuke.args4j.Option;

public class ConfigOption {

	private final static ConfigOption config = new ConfigOption();

	public static ConfigOption getInstance() {
		return config;
	}

	@Option(required = true, name = "-in", usage = "scan text path")
	private String in; // path
	@Option(required = false, name = "-out", usage = "text out")
	private String out; // name 

	public String getIn() {
		return in;
	}

	public void setIn(String in) {
		this.in = in;
	}

	public String getOut() {
		return out;
	}

	public void setOut(String out) {
		this.out = out;
	}

}
