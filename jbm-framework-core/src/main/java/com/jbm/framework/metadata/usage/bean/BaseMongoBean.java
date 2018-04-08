package com.jbm.framework.metadata.usage.bean;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

/**
 * 
 * 基础MONGO实体封装类
 * 
 * @author wesley
 *
 */
public abstract class BaseMongoBean implements PrimaryKey<String> {

	/**
	 */
	private static final long serialVersionUID = 1L;

	private @Id String _id;

	public final String getId() {
		return _id;
	}

	public final void setId(String id) {
		this._id = id;
	}

	public BaseMongoBean() {
		super();
	}

	public BaseMongoBean(String _id) {
		super();
		this._id = _id;
	}

	public final ObjectId convertObjectId() {
		return new ObjectId(_id);
	}

}
