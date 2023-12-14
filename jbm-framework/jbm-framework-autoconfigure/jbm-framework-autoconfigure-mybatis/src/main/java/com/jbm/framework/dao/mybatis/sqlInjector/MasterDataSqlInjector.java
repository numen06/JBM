package com.jbm.framework.dao.mybatis.sqlInjector;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;

import java.util.List;

/**
 * @author wesley.zhang
 */
public class MasterDataSqlInjector extends DefaultSqlInjector {
    public final static String SELECT_BY_CODE = "selectByCode";

    public final static String UPDATE_BY_CODE = "updateByCode";

    public final static String DELETE_BY_CODE = "deleteByCode";


    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
        List<AbstractMethod> methods = super.getMethodList(mapperClass, tableInfo);
        methods.add(new SelectByCode(SELECT_BY_CODE));
        methods.add(new UpdateByCode(UPDATE_BY_CODE));
        methods.add(new DeleteByCode(DELETE_BY_CODE));
        return methods;
    }
}
