package com.td.framework.devops.actuator.bean;

public class SigarInfoEntity {
	private String value;
	private String name;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SigarInfoEntity(String value, String name) {
		super();
		this.value = value;
		this.name = name;
	}

	public SigarInfoEntity() {

	}
}
