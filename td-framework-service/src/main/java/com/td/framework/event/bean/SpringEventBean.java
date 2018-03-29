package com.td.framework.event.bean;

import java.util.Date;

import org.springframework.context.ApplicationEvent;

import com.td.framework.metadata.usage.bean.PrimaryKey;

/**
 * 时间包装类
 * 
 * @author wesley
 *
 */
public class SpringEventBean extends ApplicationEvent implements PrimaryKey<String> {

	private String id;
	private Date createTime = null;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public SpringEventBean(Object source) {
		super(source);
	}

	public SpringEventBean(Object source, String id) {
		super(source);
		this.id = id;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
