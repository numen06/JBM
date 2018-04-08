package com.jbm.framework.masterslave.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 
 * 数据库接管失误管理拦截器
 * 
 * @author Wesley
 * 
 */
public class MasterSlaveTransactionManager extends DataSourceTransactionManager {

	private final static Logger logger = LoggerFactory.getLogger(MasterSlaveTransactionManager.class);

	@Autowired
	private IDataPreparationRespository dataPreparationRespository;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		super.doBegin(transaction, definition);
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		String key = dataPreparationRespository.buildTransactionId(TransactionSynchronizationManager.getCurrentTransactionName(),
			TransactionSynchronizationManager.getSynchronizations());
		super.doCommit(status);
		logger.debug("最终提交事务:" + key);
		dataPreparationRespository.transportData(key);
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		String key = dataPreparationRespository.buildTransactionId(TransactionSynchronizationManager.getCurrentTransactionName(),
			TransactionSynchronizationManager.getSynchronizations());
		super.doRollback(status);
		logger.debug("最终回滚事务:" + key);
		dataPreparationRespository.abandonData(key);
	}

}
