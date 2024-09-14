package com.jbm.util.key;

import cn.hutool.core.util.TypeUtil;


/**
 * @author wesley
 */
public class KeyBean<T> extends KeyObject {


    private Class<T> beanType;

    public KeyBean(T value) {
        super(value);
    }

    public Class<T> getBeanType() {
        if (beanType == null) {
            // 获取当前类的第一个泛型参数的类型
            beanType = (Class<T>) TypeUtil.getTypeArgument(this.getClass(), 0);
        }
        return beanType;
    }

    public T to() {
        return super.to(this.getBeanType());
    }
}
