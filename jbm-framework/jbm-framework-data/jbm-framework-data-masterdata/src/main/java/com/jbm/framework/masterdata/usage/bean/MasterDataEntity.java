package com.jbm.framework.masterdata.usage.bean;

import javax.persistence.MappedSuperclass;

/**
 * 基础类模型
 * 
 * @author wesley
 *
 * @param <CODE>
 */
@MappedSuperclass
public class MasterDataEntity extends AdvEntity<String> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
