package com.jbm.opcua.test;


import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.framework.opcua.OpcUaTemplate;
import jbm.framework.boot.autoconfigure.opcua.OpcUaConfiguration;
import jbm.framework.boot.autoconfigure.opcua.OpcUaProperties;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.AddNodesItem;
import org.eclipse.milo.opcua.stack.core.types.structured.Node;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AbstractTestContextBootstrapper;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import java.util.List;
import java.util.concurrent.ExecutionException;


@RunWith(SpringRunner.class)
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
