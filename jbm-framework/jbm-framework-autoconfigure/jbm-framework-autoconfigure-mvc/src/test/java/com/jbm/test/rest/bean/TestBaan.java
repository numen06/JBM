package com.jbm.test.rest.bean;

import com.alibaba.fastjson.annotation.JSONField;
import jbm.framework.boot.autoconfigure.fastjson.serializer.EnumValueDeserializer;
import lombok.Data;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-15 00:31
 **/
@Data
public class TestBaan {

    private DicEnumTest dicEnumTest;
}
