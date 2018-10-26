package com.jbm.metadata.entity.common;

import com.jbm.metadata.entity.common.type.IMetaType;

/**
 * 实体基础状态
 * 
 * @author wesley
 *
 */
public class MetaState implements IMetaType {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4116544719393995545L;

	private Integer code;

	private String name;

	public MetaState() {
		super();
	}

	public MetaState(Integer code, String name) {
		super();
		this.code = code;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

}
