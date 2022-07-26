package com.jbm.framework.dao.mybatis.sqlInjector;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;

import java.util.List;

/**
 * @author wesley.zhang
 */
public class MasterDataSqlInjector extends DefaultSqlInjector {
    public final static String SELECT_BY_CODE = "selectByCode";

    public final static String UPDATE_BY_CODE = "updateByCode";

    public final static String DELETE_BY_CODE = "deleteByCode";

    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass) {
        List<AbstractMethod> methods = super.getMethodList(mapperClass);
        methods.add(new SelectByCode());
        methods.add(new UpdateByCode());
        methods.add(new DeleteByCode());
        return methods;
    }

}
