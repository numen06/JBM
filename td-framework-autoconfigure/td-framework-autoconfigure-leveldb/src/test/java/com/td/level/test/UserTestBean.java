package com.td.level.test;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@SuppressWarnings("serial")
public class UserTestBean implements Serializable {

	private String id;

	private String name;

	private Integer age;

	private Date createTime = new Date();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public static UserTestBean newBean() {
		UserTestBean bean = new UserTestBean();
		bean.setId(UUID.randomUUID().toString());
		bean.setAge(System.identityHashCode(bean));
		bean.setName(UUID.randomUUID().toString());
		return bean;
	}
}
