package com.jbm.framework.metadata.usage.page;

import com.jbm.util.map.ParameterMap;

/**
 * 分组数据封装类
 * 
 * @author Wesley
 * 
 */
public class GroupCond extends ParameterMap<String, Object> {

	private static final long serialVersionUID = 1L;
	/**
	 * 列名
	 */
	private String column;

	public GroupCond(String column) {
		super();
		this.column = column;
		this.append(column, true);
	}

	public String getColumn() {
		return column;
	}

	public void remove() {
		super.remove(column);
	}

}
