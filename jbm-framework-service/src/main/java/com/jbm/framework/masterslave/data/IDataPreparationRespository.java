package com.jbm.framework.masterslave.data;

import org.apache.ibatis.mapping.MappedStatement;

import com.jbm.framework.masterslave.ProjectConfig;

public interface IDataPreparationRespository {

	ProjectConfig getProjectConfig();

	void proceed(MappedStatement mappedStatement, boolean realSuccess, String currentTransactionName, Object synchronizations, MappedStatement statement, Object parameter);

	/**
	 * 组建一个事务ID
	 * 
	 * @param currentTransactionName
	 * @param synchronizations
	 * @return
	 */
	String buildTransactionId(String currentTransactionName, Object synchronizations);

	void transportData(String key);

	void abandonData(String key);

}
