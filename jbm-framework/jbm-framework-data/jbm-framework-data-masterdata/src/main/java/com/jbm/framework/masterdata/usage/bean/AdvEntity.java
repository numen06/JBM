package com.jbm.framework.masterdata.usage.bean;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * 
 * 封装ID和CODE两大唯一键的高级实体
 * 
 * @author wesley
 *
 */
@MappedSuperclass
public class AdvEntity extends BaseEntity implements CodePrimaryKey<Long, String> {
	/**
	 * 	
	 */
	private static final long serialVersionUID = 4915439801688748572L;

	@Column
	private String code;

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public void setCode(String code) {
		this.code = code;
	}

	public AdvEntity() {
		super();
	}

	public AdvEntity(String code) {
		super();
		this.code = code;
	}

	public AdvEntity(Long id, String code) {
		super(id);
		this.code = code;
	}

}
