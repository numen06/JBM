package com.jbm.util.key;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.TypeUtil;


/**
 * @author wesley
 */
public class KeyBean<T> extends KeyObject {


    private Class<T> beanType;

    public KeyBean(T value) {
        super(value);
        this.beanType = ClassUtil.getClass(value);
    }

    public Class<T> getBeanType() {
        return beanType;
    }

    public T to() {
        return super.to(this.getBeanType());
    }
}
