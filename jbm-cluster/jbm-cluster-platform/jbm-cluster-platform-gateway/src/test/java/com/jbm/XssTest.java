package com.jbm;

import cn.hutool.core.io.resource.ResourceUtil;
import com.jbm.cluster.platform.gateway.filter.XssFilter;
import org.junit.jupiter.api.Test;

public class XssTest {

    @Test
    public void testClear() {
        String html = ResourceUtil.readUtf8Str("test.html");
        //获取body标签的内容
        String softHtml = XssFilter.filter(html);
        //对比一下两者差异
        System.out.println(html.equals(softHtml));
        System.out.println("--------------------");
        System.out.println(html);
        System.out.println("--------------------");
        System.out.println(softHtml);
        System.out.println("--------------------");
        //查询一下差异点
        System.out.println(html.indexOf(softHtml));

    }

    @Test
    public void testClear2() {
         String html = "{\"userId\":\"0\",\"appId\":\"1985282382643187713\"}";
         String softHtml = XssFilter.filter(html);
         System.out.println(html.equals(softHtml));

    }
}
