package com.jbm.util.key;

import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.lang.func.LambdaUtil;
import cn.hutool.core.util.ClassUtil;

import java.util.HashMap;
import java.util.Map;


/**
 * @author wesley
 */
public class KeyBean<T> extends KeyObject {

    private Class<T> beanType;

    public KeyBean() {
        super();
    }

    public KeyBean(T value) {
        super(value);
        this.beanType = ClassUtil.getClass(value);
    }

    @SafeVarargs
    public KeyBean(T value, Func1<T, ?>... keyFunctions) {
        super(() -> {
            Map<String, Object> map = new HashMap<>();
            for (Func1<T, ?> keyFunction : keyFunctions) {
                if (keyFunction != null) {
                    String mName = LambdaUtil.getFieldName(keyFunction);
                    map.put(mName, keyFunction.callWithRuntimeException(value));
                }
            }
            return map;
        });
        this.beanType = ClassUtil.getClass(value);
    }

    public Class<T> getBeanType() {
        return beanType;
    }

    public T to() {
        return super.to(this.getBeanType());
    }


}
