package com.jbm.framework.bo.usage;

import java.io.Serializable;

import com.jbm.framework.metadata.exceptions.BizException;

public interface IBo<P extends IBizParam, A extends IBizAction> extends Serializable {

	/**
	 * 获取BO的序列号
	 * 
	 * @return
	 */
	String getSn();

	P getParam();

	A getAction();

	/**
	 * 初始化当前BO
	 */
	void initialize() throws BizException;

}
