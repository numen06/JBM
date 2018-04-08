package com.jbm.framework.metadata.usage.bean;

import java.io.Serializable;

/**
 * 标示主键
 * 
 * @author Wesley
 * 
 * @param <ID>
 */
public interface PrimaryKey<ID extends Serializable> extends Serializable {

	ID getId();

	void setId(ID id);
}
