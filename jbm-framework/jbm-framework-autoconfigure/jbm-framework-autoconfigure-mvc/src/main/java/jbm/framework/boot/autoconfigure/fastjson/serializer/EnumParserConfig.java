package jbm.framework.boot.autoconfigure.fastjson.serializer;

import com.alibaba.fastjson.annotation.JSONType;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;

import java.lang.reflect.Type;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-03-15 00:55
 **/
public class EnumParserConfig extends ParserConfig {


    @Override
    public ObjectDeserializer getDeserializer(Class<?> clazz, Type type) {
        ObjectDeserializer derializer;
        if (clazz.isEnum()) {
            Class<?> deserClass = null;
            JSONType jsonType = clazz.getAnnotation(JSONType.class);
            if (jsonType != null) {
                deserClass = jsonType.deserializer();
                try {
                    derializer = (ObjectDeserializer) deserClass.newInstance();
                    this.putDeserializer(clazz, derializer);
                    return derializer;
                } catch (Throwable error) {
                    // skip
                }
            }
            derializer = new EnumValueDeserializer();
            return derializer;
        }
        return super.getDeserializer(clazz, type);
    }
}
