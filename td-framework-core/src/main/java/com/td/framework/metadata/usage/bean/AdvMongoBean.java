package com.td.framework.metadata.usage.bean;

import java.io.Serializable;

import org.springframework.data.mongodb.core.index.Indexed;

/**
 * 
 * 高级MONGO封装实体类
 * 
 * @author wesley
 *
 * @param <CODE>
 */
public class AdvMongoBean<CODE extends Serializable> extends BaseMongoBean implements CodePrimaryKey<String, CODE> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4915439801688748572L;
	private @Indexed CODE code;

	@Override
	public CODE getCode() {
		return code;
	}

	@Override
	public void setCode(CODE code) {
		this.code = code;
	}

	public AdvMongoBean() {
		super();
	}

	public AdvMongoBean(CODE code) {
		super();
		this.code = code;
	}

	public AdvMongoBean(String id, CODE code) {
		super(id);
		this.code = code;
	}

}
