package jbm.framework.boot.autoconfigure.fastjson.serializer;

import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.baomidou.mybatisplus.annotation.EnumValue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-14 21:26
 **/
public class EnumValueSerializer implements ObjectSerializer {

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.out;
        Field field = this.getFieldAnnotationField((Class<?>) fieldType);
        if (field == null)
            out.writeEnum((Enum<?>) object);
        else {
            serializer.write(ReflectUtil.getFieldValue(object, field));
        }
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
