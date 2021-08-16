package com.jbm.framework.masterdata.usage;

import com.jbm.framework.masterdata.usage.bean.EntityMap;

public interface EnumConvertInterceptor {
    boolean convert(EntityMap map, String key, Object v);
}
