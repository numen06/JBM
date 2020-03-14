package jbm.framework.boot.autoconfigure.fastjson.serializer;

import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.EnumDeserializer;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.baomidou.mybatisplus.annotation.EnumValue;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-14 21:26
 **/
public class EnumValueDeserializer implements ObjectDeserializer {


    private EnumDeserializer enumDeserializer;

    public EnumValueDeserializer(Class<?> clazz) {
        this.enumDeserializer = new EnumDeserializer(clazz);
    }

    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        Field field = this.getFieldAnnotationField((Class<?>) type);
        if (field == null) {
            return enumDeserializer.deserialze(parser, type, fieldName);
        }
        Class cls = (Class) type;
        Object[] enumConstants = cls.getEnumConstants();
        Object o = parser.parseObject(field.getType(), fieldName);
        for (Object enumConstant : enumConstants) {
            if (ReflectUtil.getFieldValue(enumConstant, field).equals(o)) {
                return (T) enumConstant;
            }
        }
        return null;
    }

    @Override
    public int getFastMatchToken() {
        return JSONToken.LITERAL_INT;
    }

    public Field getFieldAnnotationField(Class<?> fieldType) throws SecurityException {
        // 获取所有的字段
        Field[] fields = fieldType.getDeclaredFields();
        for (Field f : fields) {
            // 判断字段注解是否存在
            boolean has = f.isAnnotationPresent(EnumValue.class);
            if (has) {
                return f;
            }
        }
        return null;
    }


}
