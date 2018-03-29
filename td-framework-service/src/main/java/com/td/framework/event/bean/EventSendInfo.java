package com.td.framework.event.bean;

import java.io.Serializable;
import java.util.Date;

public class EventSendInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String topic;

	private Date sendTime;

	private Long total;

	public EventSendInfo(String topic, Date sendTime, Long total) {
		super();
		this.topic = topic;
		this.sendTime = sendTime;
		this.total = total;
	}

	public Date getSendTime() {
		return sendTime;
	}

	public void setSendTime(Date sendTime) {
		this.sendTime = sendTime;
	}

	public Long getTotal() {
		return total;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public void setTotal(Long total) {
		this.total = total;
	}

}
