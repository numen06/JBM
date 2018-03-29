package com.td.framework.event.bean;

import java.util.Date;
import java.util.UUID;

import org.springframework.context.ApplicationEvent;

import com.td.framework.metadata.usage.bean.PrimaryKey;

/**
 * 时间包装类
 * 
 * @author wesley
 *
 */
public class RemoteEventBean extends ApplicationEvent implements PrimaryKey<String> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String id = UUID.randomUUID().toString();
	private Date createTime = new Date();
	private Date receiveTime = null;
	private String group = null;

	public RemoteEventBean(Object source) {
		super(source);
	}

	public RemoteEventBean(Object source, String group) {
		super(source);
		this.group = group;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public Object getSource() {
		return source;
	}

	public void setSource(Object source) {
		this.source = source;
	}

	private Object data;

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getReceiveTime() {
		return receiveTime;
	}

	public void setReceiveTime(Date receiveTime) {
		this.receiveTime = receiveTime;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
