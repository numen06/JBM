package com.td.metadata.entity.common;

import java.io.Serializable;

/**
 * @author wesley
 *
 * @param <CODE>
 */
public class MetaLevelEntity<CODE extends Serializable> extends MetaEntity<CODE> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5481321341018117971L;
	/**
	 * 上级指向
	 */
	private CODE parentCode;

	/**
	 * 层级
	 */
	private Integer level;

	public CODE getParentCode() {
		return parentCode;
	}

	public void setParentCode(CODE parentCode) {
		this.parentCode = parentCode;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

}
