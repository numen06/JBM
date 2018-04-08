package com.jbm.masterdata.entity.common;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import com.jbm.framework.metadata.usage.bean.AdvEntity;

/**
 * 基础类模型
 * 
 * @author wesley
 *
 * @param <CODE>
 */
@MappedSuperclass
public class MasterEntity<CODE extends Serializable> extends AdvEntity<CODE> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2859718235308439898L;

	@Column
	private String name;
	@Column
	private Long followSourceId;

	/**
	 * 状态
	 */
	@Column
	private Integer state;

	public Integer getState() {
		return state;
	}

	public void setState(Integer state) {
		this.state = state;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getFollowSourceId() {
		return followSourceId;
	}

	public void setFollowSourceId(Long followSourceId) {
		this.followSourceId = followSourceId;
	}
	
	

}
