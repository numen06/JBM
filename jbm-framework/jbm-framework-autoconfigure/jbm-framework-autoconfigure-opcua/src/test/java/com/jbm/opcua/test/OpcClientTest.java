package com.jbm.opcua.test;


import com.alibaba.fastjson.JSON;
import com.jbm.framework.opcua.OpcUaTemplate;
import org.junit.Test;

public class OpcClientTest {

    private OpcUaTemplate opcUaTemplate = new OpcUaTemplate();

    @Test
    public void array() throws Exception {
//        opcUaTemplate.readItem("deviceId", "点位", byte[].class);
        String value = JSON.toJSONString("test".getBytes());
        System.out.println(new String(JSON.parseObject(value, byte[].class)));
    }

    @Test
    public void intwet() {
        System.out.println(JSON.toJSONString(1));
    }

}
