package com.jbm.framework.masterdata.usage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-25 01:03
 **/
public class ClassQueryWrapper<T> extends QueryWrapper<T> {

    public ClassQueryWrapper() {
        super();
    }

    public ClassQueryWrapper(Class<T> clazz) {
        super();
//        super.setEntityClass(clazz);
    }

    public ClassQueryWrapper(T entity) {
        super(entity);
    }

    public ClassQueryWrapper(T entity, String... columns) {
        super(entity, columns);
    }

    public static <T> QueryWrapper<T> QueryWrapper(Class<T> clazz) {
        return new ClassQueryWrapper(clazz);
    }
}
