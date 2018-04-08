package com.jbm.framework.metadata.usage.bean;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableLogic;
import com.baomidou.mybatisplus.enums.IdType;

/**
 * 技术的实体
 * 
 * @author wesley
 *
 */
@MappedSuperclass
public abstract class BaseEntity implements PrimaryKey<Long> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7148367690448503947L;

	@Id
	@GeneratedValue
	@TableId(type = IdType.ID_WORKER)
	private Long id;
	
	
	@TableField(value = "delete_flag")
	@TableLogic
	private Integer deleteFlag;

	public BaseEntity() {
		super();
	}

	public BaseEntity(Long id) {
		super();
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getDeleteFlag() {
		return deleteFlag;
	}

	public void setDeleteFlag(Integer deleteFlag) {
		this.deleteFlag = deleteFlag;
	}
	
	

}
