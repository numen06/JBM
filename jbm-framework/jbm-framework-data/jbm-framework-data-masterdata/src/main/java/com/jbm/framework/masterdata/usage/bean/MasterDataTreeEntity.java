package com.jbm.framework.masterdata.usage.bean;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * @author wesley
 *
 * @param <CODE>
 */
@MappedSuperclass
public class MasterDataTreeEntity extends MasterDataEntity {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 上级指向
	 */
	@Column
	private String parentCode;

	@Column
	private Long parentId;

	/**
	 * 层级
	 */
	@Column
	private Integer level;

	public String getParentCode() {
		return parentCode;
	}

	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

}
