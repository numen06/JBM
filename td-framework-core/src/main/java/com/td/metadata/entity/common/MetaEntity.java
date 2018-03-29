package com.td.metadata.entity.common;

import java.io.Serializable;

import org.springframework.data.mongodb.core.index.Indexed;

import com.td.framework.metadata.usage.bean.AdvMongoBean;

/**
 * 基础类模型
 * 
 * @author wesley
 *
 * @param <CODE>
 */
public class MetaEntity<CODE extends Serializable> extends AdvMongoBean<CODE> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2859718235308439898L;

	/**
	 * 名称
	 */
	@Indexed
	private String name;

	private String appKey;

	/**
	 * 状态
	 */
	@Indexed
	private MetaState metaState;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MetaState getMetaState() {
		return metaState;
	}

	public void setMetaState(MetaState metaState) {
		this.metaState = metaState;
	}

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

}
