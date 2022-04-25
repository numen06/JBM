package com.jbm.test.rest;

import com.alibaba.fastjson.JSON;
import com.jbm.test.rest.bean.DicEnumTest;
import com.jbm.test.rest.bean.TestBaan;
import jbm.framework.boot.autoconfigure.fastjson.serializer.EnumParserConfig;
import jbm.framework.boot.autoconfigure.fastjson.serializer.EnumSerializeConfig;
import org.junit.jupiter.api.Test;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-15 00:27
 **/
public class EnumTest {


    @Test
    public void enumTest() {
        TestBaan baan = new TestBaan();
        baan.setDicEnumTest(DicEnumTest.cte);
        System.out.println(JSON.toJSONString(baan, new EnumSerializeConfig()));
    }

    @Test
    public void enumTest2() {
        String test = "{\"dicEnumTest\":1}";
        TestBaan baan = JSON.parseObject(test, TestBaan.class, new EnumParserConfig());
        System.out.println(baan);
        System.out.println(JSON.toJSONString(baan, new EnumSerializeConfig()));
    }


    @Test
    public void enumTest3() {
        String test = "{\"dicEnumTest\":2}";
        TestBaan baan = JSON.parseObject(test, TestBaan.class, new EnumParserConfig());
        System.out.println(baan);
        System.out.println(JSON.toJSONString(baan, new EnumSerializeConfig()));
    }
}
