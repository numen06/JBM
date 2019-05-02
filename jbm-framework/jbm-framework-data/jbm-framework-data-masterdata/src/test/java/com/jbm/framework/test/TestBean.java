package com.jbm.framework.test;

import com.alibaba.fastjson.JSON;
import com.github.jsonzou.jmockdata.JMockData;
import com.github.jsonzou.jmockdata.MockConfig;
import com.jbm.framework.test.bean.AXB;
import com.jbm.framework.test.bean.BasicBean;
import com.jbm.framework.test.bean.SelfRefData;
import org.junit.Test;

/**
 * @author: create by wesley
 * @date:2019/5/2
 */
public class TestBean {

    @Test
    public void testSelf() {
        MockConfig mockConfig = MockConfig.newInstance().globalConfig().setEnabledCircle(true);
        SelfRefData selfRefData = JMockData.mock(SelfRefData.class, mockConfig);
//        System.out.println(JSON.toJSONString(selfRefData));
    }

    @Test
    public void testBasic() {
        MockConfig mockConfig = MockConfig.newInstance().globalConfig().setEnabledCircle(true);
        BasicBean basicBean = JMockData.mock(BasicBean.class, mockConfig);
        System.out.println(JSON.toJSONString(basicBean));
    }

    @Test
    public void testCircular() {
        MockConfig mockConfig = MockConfig.newInstance().globalConfig().setEnabledCircle(true);
        AXB axb = JMockData.mock(AXB.class, mockConfig);
        AXB circularAxb = axb.getBXA().getAXB();
        System.out.println(JSON.toJSONString(axb));
        System.out.println(JSON.toJSONString(circularAxb));
    }
}
