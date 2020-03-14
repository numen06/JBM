package jbm.framework.boot.autoconfigure.fastjson.serializer;

import com.alibaba.fastjson.annotation.JSONType;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.util.TypeUtils;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-15 00:58
 **/
public class EnumSerializeConfig extends SerializeConfig {

    @Override
    public ObjectSerializer getObjectWriter(Class<?> clazz) {
        ObjectSerializer writer = this.get(clazz);
        if (clazz.isEnum()) {
            JSONType jsonType = TypeUtils.getAnnotation(clazz, JSONType.class);
            if (jsonType != null && jsonType.serializeEnumAsJavaBean()) {
                put(clazz, writer = createJavaBeanSerializer(clazz));
            } else {
                put(clazz, writer = new EnumValueSerializer());
            }
            return writer;
        }
        return super.getObjectWriter(clazz);
    }
}
