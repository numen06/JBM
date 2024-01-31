package com.jbm.test.rest;

import com.alibaba.fastjson.JSON;
import org.junit.Test;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-09-22 14:08
 **/
public class FastJsonTest {

    @Test
    public void test() {
        TestClass testClass = JSON.parseObject("{name:\"\"}", TestClass.class);
        System.out.println(JSON.toJSONString(testClass));
    }
}
