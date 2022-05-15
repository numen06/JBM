package com.jbm.opcua.test;


import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.framework.opcua.OpcUaTemplate;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;


@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@SpringBootTest(classes = {ApplicationTest.class})
@Slf4j
public class OpcClientTest {

    @Autowired
    private OpcUaTemplate opcUaTemplate;


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

    @Test
    public void testClient() throws Exception {
//        String value = this.opcUaTemplate.readItem("test", "Int32");
        OpcUaClient client = this.opcUaTemplate.getOpcUaClient("test");
//        System.out.println(value);

        client.connect().get();

        List<ReferenceDescription> nodes = client.getAddressSpace().browse(Identifiers.RootFolder);

        while (true) {
//            for (ReferenceDescription node : nodes) {
//                System.out.println("Node= " + node.getNodeId());
//            }
            try {
                System.out.println(this.opcUaTemplate.getOpcUaClient("test").getSession().get().getSessionName());
            } catch (Exception e) {
                System.out.println(e);
            }
            ThreadUtil.safeSleep(2000);
        }
    }
}
