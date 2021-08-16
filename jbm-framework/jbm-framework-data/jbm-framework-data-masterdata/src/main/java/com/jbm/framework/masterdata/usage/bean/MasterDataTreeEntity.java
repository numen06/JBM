package com.jbm.framework.masterdata.usage.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * @author wesley
 *
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

	//	@Transient
	@TableField(exist = false)
	private Boolean leaf;
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

	public Boolean getLeaf() {
		return leaf;
	}

	public void setLeaf(Boolean leaf) {
		this.leaf = leaf;
	}



}
