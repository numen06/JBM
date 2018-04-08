package com.jbm.framework.devops.client.bean;

import org.kohsuke.args4j.Option;

public class ConfigOption {

	private final static ConfigOption config = new ConfigOption();

	public static ConfigOption getInstance() {
		return config;
	}

	@Option(required = false, name = "-p", usage = "scan source path")
	private String path; // path
	@Option(required = false, name = "-n", usage = "source name")
	private String name; // name
	@Option(required = false, name = "-r", usage = "post url to release")
	private Boolean release = true; // release
	@Option(required = true, name = "-url", usage = "update url to actuator")
	private String url; // url
	@Option(required = false, name = "-s", usage = "release or start")
	private Boolean start = false; // start
	@Option(required = false, name = "-doc", usage = "the doc url")
	private String doc; // doc url

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getRelease() {
		return release;
	}

	public void setRelease(Boolean release) {
		this.release = release;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Boolean getStart() {
		return start;
	}

	public void setStart(Boolean start) {
		this.start = start;
	}

	public String getDoc() {
		return doc;
	}

	public void setDoc(String doc) {
		this.doc = doc;
	}

}
