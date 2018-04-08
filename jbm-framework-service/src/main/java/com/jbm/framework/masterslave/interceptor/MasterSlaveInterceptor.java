package com.jbm.framework.masterslave.interceptor;

import java.util.Properties;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.jbm.framework.masterslave.data.IDataPreparationRespository;
import com.jbm.util.ObjectUtils;
import com.jbm.util.PatternMatchUtils;
import com.jbm.util.StringUtils;

/**
 * 
 * 主从增删改拦截器
 * 
 * @author Wesley
 * 
 */
@Intercepts({ @Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }) })
public class MasterSlaveInterceptor implements Interceptor {

	private final static Logger logger = LoggerFactory.getLogger(MasterSlaveInterceptor.class);

	protected Properties properties;
	protected static final int MAPPED_STATEMENT_INDEX = 0;
	protected static final int PARAMETER_INDEX = 1;
	protected static final int ROWBOUNDS_INDEX = 2;
	protected static final int RESULT_HANDLER_INDEX = 3;

	private static IDataPreparationRespository dataPreparationRespository;

	public static void setDataPreparationRespository(IDataPreparationRespository dataPreparationRespository) {
		MasterSlaveInterceptor.dataPreparationRespository = dataPreparationRespository;
	}

	private String[] whiteList;

	public MasterSlaveInterceptor() {
		super();
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {

		final Object[] args = invocation.getArgs();
		final MappedStatement statement = (MappedStatement) args[MAPPED_STATEMENT_INDEX];
		final Object parameter = args[PARAMETER_INDEX];

		// if (DemoTest.applicationContext_pub != null)
		// dataPreparationRespository =
		// DemoTest.applicationContext_pub.getBean(DataPreparationRespository.class);

		Object returnValue = null;
		boolean realSuccess = true;
		try {
			returnValue = invocation.proceed();
		} catch (Exception e) {
			realSuccess = false;
			throw e;
		} finally {
			try {
				if (ObjectUtils.isNull(dataPreparationRespository))
					return returnValue;
				if (!dataPreparationRespository.getProjectConfig().getMasterSlave())
					return returnValue;
				if (!checkIntercept(statement.getId()))
					return returnValue;
				if (TransactionSynchronizationManager.isSynchronizationActive())
					dataPreparationRespository.proceed(statement, realSuccess, TransactionSynchronizationManager.getCurrentTransactionName(),
						TransactionSynchronizationManager.getSynchronizations(), statement, parameter);
				else
					dataPreparationRespository.proceed(statement, realSuccess, null, null, statement, parameter);
			} catch (Exception e) {
				logger.error("主从拦截错误", e);
			}
		}
		return returnValue;
	}

	protected boolean checkIntercept(String statement) {
		for (int i = 0; i < whiteList.length; i++) {
			boolean intercept = PatternMatchUtils.simpleMatch(whiteList[i], statement);
			if (intercept)
				return false;
		}
		return true;
	}

	@Override
	public Object plugin(Object target) {
		// dataPreparationRespository =
		// DemoTest.applicationContext_pub.getBean(DataPreparationRespository.class);
		return Plugin.wrap(target, this);
	}

	@Override
	public void setProperties(Properties properties) {
		this.properties = properties;
		whiteList = StringUtils.splitToEmpty(properties.getProperty("whiteList"));
	}

}
