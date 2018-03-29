package com.td.framework.metadata.enumerate;

/**
 * 数据库操作方式
 * 
 * @author Wesley
 * 
 */
public enum SqlOperateEnum {

	select(0), insert(1), update(2), delete(3);

	private Integer Value;

	private SqlOperateEnum(Integer value) {
		this.Value = value;
	}

	public Integer getValue() {
		return Value;
	}

	public void setValue(Integer Value) {
		this.Value = Value;
	}

}
