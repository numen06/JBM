package com.jbm.util.version;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.jbm.util.bean.Version;

import java.lang.reflect.Type;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-03-14 21:26
 **/
public class VersionDeserializer implements ObjectDeserializer {

    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        Object value = parser.parse();
        return value == null ? null : (T) Version.parse(value.toString());
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }


}
