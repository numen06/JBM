package com.td.framework.metadata.usage.bean;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * 
 * 高级MONGO封装实体类
 * 
 * @author wesley
 *
 * @param <CODE>
 */
@MappedSuperclass
public class AdvEntity<CODE extends Serializable> extends BaseEntity implements CodePrimaryKey<Long, CODE> {
	/**
	 * 	
	 */
	private static final long serialVersionUID = 4915439801688748572L;

	@Column
	private CODE code;

	@Override
	public CODE getCode() {
		return code;
	}

	@Override
	public void setCode(CODE code) {
		this.code = code;
	}

	public AdvEntity() {
		super();
	}

	public AdvEntity(CODE code) {
		super();
		this.code = code;
	}

	public AdvEntity(Long id, CODE code) {
		super(id);
		this.code = code;
	}

}
