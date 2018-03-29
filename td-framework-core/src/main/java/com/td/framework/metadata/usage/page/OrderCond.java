package com.td.framework.metadata.usage.page;

import com.td.util.enumerate.Sort;
import com.td.util.map.ParameterMap;

public class OrderCond extends ParameterMap<String, Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 排序类型
	 */
	private String column;

	/**
	 * 排序类型
	 */
	private String sort = "";

	public OrderCond(String column) {
		this(column, "");
		super.put(column, "");
	}

	public OrderCond(String column, Sort sort) {
		this.column = column;
		this.sort = sort == Sort.ASC ? "" : Sort.DESC.toString();
		super.put(column, sort);
	}

	public OrderCond(String column, String sort) {
		this.column = column;
		this.sort = sort;
		super.put(column, sort);
	}

	public OrderCond(String column, Integer sort) {
		this.column = column;
		this.sort = sort == 0 ? "" : Sort.DESC.toString();
		super.put(column, sort);
	}

	public String getColumn() {
		return column;
	}

	public String getSort() {
		return sort;
	}

	public void remove() {
		super.remove(column);
	}
}
