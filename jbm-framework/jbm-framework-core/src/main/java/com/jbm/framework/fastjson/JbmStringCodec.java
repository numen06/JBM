package com.jbm.framework.fastjson;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.serializer.StringCodec;

import java.lang.reflect.Type;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-09-22 14:12
 **/
public class JbmStringCodec extends StringCodec {

    public static JbmStringCodec instance = new JbmStringCodec();

    public String deserialze(DefaultJSONParser parser, Type clazz, Object fieldName) {
        return StrUtil.emptyToNull(super.deserialze(parser, clazz, fieldName));
    }
}
