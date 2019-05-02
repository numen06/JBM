package com.jbm.framework.test;

import com.alibaba.fastjson.JSON;
import com.github.jsonzou.jmockdata.JMockData;
import com.jbm.framework.test.bean.SelfRefData;
import org.junit.Test;

/**
 * @author: create by wesley
 * @date:2019/5/2
 */
public class TestBean {

    @Test
    public void testSelf() {
        SelfRefData selfRefData = JMockData.mock(SelfRefData.class);
        System.out.println(JSON.toJSONString(selfRefData));

    }
}
