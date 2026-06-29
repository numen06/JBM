package jbm.framework.boot.autoconfigure.fastjson;

import cn.hutool.core.util.ReflectUtil;
import com.jbm.util.sensitive.SensitiveContext;
import com.alibaba.fastjson.serializer.ValueFilter;
import com.jbm.util.sensitive.SensitiveContext;
import com.jbm.util.sensitive.SensitiveDataUtils;
import com.jbm.util.sensitive.SensitiveField;
import com.jbm.util.sensitive.SensitiveType;

import java.lang.reflect.Field;

/**
 * Fastjson 敏感字段脱敏过滤器
 */
public class SensitiveJsonValueFilter implements ValueFilter {

    @Override
    public Object process(Object object, String name, Object value) {
        if (SensitiveContext.shouldSkipMask() || !(value instanceof String) || object == null) {
            return value;
        }
        Field field = ReflectUtil.getField(object.getClass(), name);
        if (field == null || !field.isAnnotationPresent(SensitiveField.class)) {
            return value;
        }
        SensitiveType type = field.getAnnotation(SensitiveField.class).value();
        String str = (String) value;
        switch (type) {
            case NAME:
                return SensitiveDataUtils.maskName(str);
            case MOBILE:
                return SensitiveDataUtils.maskMobile(str);
            case EMAIL:
                return SensitiveDataUtils.maskEmail(str);
            default:
                return value;
        }
    }
}
