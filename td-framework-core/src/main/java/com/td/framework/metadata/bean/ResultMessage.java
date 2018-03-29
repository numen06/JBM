package com.td.framework.metadata.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.td.framework.metadata.enumerate.MessageEnum;

/**
 * 数据库
 * 
 * @author Wesley
 * 
 */
public class ResultMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	public ResultMessage(int level) {
		super();
		this.level = level;
	}

	public ResultMessage(int level, String body) {
		super();
		this.level = level;
		this.body = body;
	}

	public ResultMessage(int level, String title, String body) {
		super();
		this.level = level;
		this.title = title;
		this.body = body;
	}

	public static List<ResultMessage> createReslutMessages(MessageEnum level, String... messages) {
		List<ResultMessage> list = new ArrayList<ResultMessage>();
		for (String s : messages) {
			list.add(new ResultMessage(level.getValue(), s));
		}
		return list;
	}

	public static List<ResultMessage> createReslutMessages(MessageEnum level, String title, String... messages) {
		List<ResultMessage> list = new ArrayList<ResultMessage>();
		for (String s : messages) {
			list.add(new ResultMessage(level.getValue(), title, s));
		}
		return list;
	}

	/**
	 * 消息等级
	 */
	private int level = 0;
	/**
	 * 标题头，可能不存在
	 */
	private String title;
	/**
	 * 显示内容，可能不存在
	 */
	private String body;

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
