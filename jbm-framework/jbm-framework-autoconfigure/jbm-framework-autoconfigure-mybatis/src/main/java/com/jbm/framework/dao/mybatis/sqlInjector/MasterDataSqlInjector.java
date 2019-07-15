package com.jbm.framework.dao.mybatis.sqlInjector;

import java.util.List;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;

/**
 * @author wesley.zhang
 *
 */
public class MasterDataSqlInjector extends DefaultSqlInjector {
	public final static String SELECT_BY_CODE ="selectByCode";

	public final static String UPDATE_BY_CODE ="UpdateByCode";

	public final static String DELETE_BY_CODE ="DeleteByCode";

	@Override
	public List<AbstractMethod> getMethodList(Class<?> mapperClass) {
		List<AbstractMethod> methods = super.getMethodList(mapperClass);
		methods.add(new SelectByCode());
		methods.add(new UpdateByCode());
		methods.add(new DeleteByCode());
		return methods;
	}

}
