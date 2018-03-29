package com.td.framework.metadata.bean;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.td.framework.metadata.enumerate.MessageEnum;
import com.td.framework.metadata.enumerate.ResultEnum;

/**
 * 后台返回给前台的封装类
 * 
 * @author Wesley
 * 
 */
public class ResultForm<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	public ResultForm(int status) {
		super();
		this.status = status;
	}

	public ResultForm(int status, T result) {
		super();
		this.status = status;
		this.result = result;
	}

	public ResultForm(int status, T result, List<ResultMessage> messages) {
		super();
		this.status = status;
		this.result = result;
		this.messages = messages;
	}

	/**
	 * 创建一个正确的返回值
	 * 
	 * @param result
	 *            返回值
	 * @param message
	 *            消息
	 * @return
	 */
	public static <T> ResultForm<T> createSuccessResultForm(T result, String... messages) {
		return createResultForm(ResultEnum.success, result, MessageEnum.success, messages);
	}

	/**
	 * 创建一个正确的返回值
	 * 
	 * @param result
	 *            返回值
	 * @param message
	 *            消息
	 * @param args
	 *            参数
	 * @return
	 */
	public static <T> ResultForm<T> success(T result, String message, String... args) {
		return createResultForm(ResultEnum.success, result, MessageEnum.success, MessageFormat.format(message, message));
	}

	/**
	 * 创建一个错误的返回值
	 * 
	 * @param result
	 *            返回值
	 * @param message
	 *            消息
	 * @return
	 */
	public static <T> ResultForm<T> createErrorResultForm(T result, String... messages) {
		return createResultForm(ResultEnum.error, result, MessageEnum.error, messages);
	}

	/**
	 * 创建一个错误的返回值
	 * 
	 * @param result
	 *            返回值
	 * @param message
	 *            消息
	 * @param args
	 *            参数
	 * @return
	 */
	public static <T> ResultForm<T> error(T result, String message, String... args) {
		return createResultForm(ResultEnum.error, result, MessageEnum.error, MessageFormat.format(message, message));
	}

	public static <T> ResultForm<T> createResultForm(ResultEnum status, T result, MessageEnum level, String... messages) {
		return new ResultForm<T>(status.getResult(), result, ResultMessage.createReslutMessages(level, messages));
	}

	public static <T> ResultForm<T> createResultForm(ResultEnum status, T result, List<ResultMessage> messages) {
		return new ResultForm<T>(status.getResult(), result, messages);
	}

	public static <T> ResultForm<T> createResultForm(ResultEnum status, T result) {
		return new ResultForm<T>(status.getResult(), result);
	}

	public static <T> ResultForm<T> createResultForm(ResultEnum status) {
		return new ResultForm<T>(status.getResult());
	}

	public static <T> ResultForm<T> createResultForm(Class<T> clazz) {
		return new ResultForm<T>(ResultEnum.none.getResult());
	}

	public static <T> ResultForm<T> createResultForm(T result) {
		return new ResultForm<T>(ResultEnum.none.getResult(), result);
	}

	private String key;

	/**
	 * 返回状态
	 */
	private int status = 0;
	/**
	 * 返回值
	 */
	private T result;
	/**
	 * 返回消息
	 */
	private List<ResultMessage> messages = new ArrayList<ResultMessage>();

	public T getResult() {
		return result;
	}

	public void setResult(T result) {
		this.result = result;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getMessage() {
		StringBuffer ms = new StringBuffer();
		for (ResultMessage messgae : this.messages) {
			ms.append(messgae.getBody());
		}
		return ms.toString();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setMessage(String message) {
		this.messages.clear();
		messages.add(new ResultMessage(this.status, message));
	}

	public ResultForm<T> addMesage(ResultEnum level, String message) {
		this.status = level.getResult();
		messages.add(new ResultMessage(level.getResult(), message));
		return this;
	}

	public ResultForm<T> addMesage(MessageEnum level, String message) {
		messages.add(new ResultMessage(level.getValue(), message));
		return this;
	}

	public List<ResultMessage> lookMessages() {
		return this.messages;
	}

	public boolean isSuccess() {
		return this.status == ResultEnum.success.getResult();
	}
}
