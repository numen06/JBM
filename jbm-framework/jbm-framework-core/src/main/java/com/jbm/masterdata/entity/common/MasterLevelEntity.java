package com.jbm.masterdata.entity.common;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * @author wesley
 *
 * @param <CODE>
 */
@MappedSuperclass
public class MasterLevelEntity<CODE extends Serializable> extends MasterEntity<CODE> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5481321341018117971L;
	/**
	 * 上级指向
	 */
	@Column
	private CODE parentCode;

	/**
	 * 层级
	 */
	@Column
	private Integer level;

	private Long parentId;

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

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
