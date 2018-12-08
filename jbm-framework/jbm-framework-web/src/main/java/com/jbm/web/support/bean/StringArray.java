package com.jbm.web.support.bean;

import java.io.Serializable;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.jbm.util.StringUtils;

/**
 * 将字符串转为数组
 * 
 * @author wesley
 *
 * @param <T>
 */
public class StringArray<T extends Serializable> implements Serializable {
	private String source = null;
	private List<T> array = Lists.newArrayList();
	private final static String COMMA = ",";
	private static final long serialVersionUID = 1L;

	public StringArray() {
		super();
	}

	public StringArray(String source) {
		super();
		this.source = source;
		this.array = this.fromString(source);
	}

	public StringArray(T[] array) {
		super();
		this.array = Lists.newArrayList(array);
		this.source = this.fromArray(array);
	}

	public StringArray(List<T> array) {
		super();
		this.array = array;
		this.source = this.fromList(array);
	}

	public StringArray(String source, List<T> list) {
		super();
		this.source = source;
		this.array = list;
	}

	@SuppressWarnings("unchecked")
	public List<T> fromString(String source) {
		return JSON.parseObject("[" + source + "]", array.getClass());
	}

	public String fromArray(T[] array) {
		return StringUtils.join(array, COMMA);
	}

	public String fromList(List<T> list) {
		return StringUtils.join(array, COMMA);
	}

	public String getSource() {
		return source;
	}

	public List<T> getArray() {
		return array;
	}

}
