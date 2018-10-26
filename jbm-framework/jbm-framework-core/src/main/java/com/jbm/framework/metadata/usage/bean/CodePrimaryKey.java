package com.jbm.framework.metadata.usage.bean;

import java.io.Serializable;

/**
 * 存在类型code的接口
 * 
 * @author wesley
 *
 * @param <ID>
 * @param <CODE>
 */
public interface CodePrimaryKey<ID extends Serializable, CODE> extends PrimaryKey<ID> {
	/**
	 * 表示自定义主键
	 * 
	 * @return
	 */
	CODE getCode();

	/**
	 * 设置自定义主键
	 * 
	 * @param code
	 */
	void setCode(CODE code);
}
