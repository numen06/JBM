package com.jbm.opcua.test;

import com.jbm.framework.opcua.OpcUaTemplate;
import jbm.framework.boot.autoconfigure.opcua.OpcUaConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackageClasses = OpcUaConfiguration.class)
public class ApplicationTest {

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(ApplicationTest.class, args);
        OpcUaTemplate opcUaTemplate = applicationContext.getBean(OpcUaTemplate.class);
        System.out.println("s1 = " + opcUaTemplate.readItem("s1", "PLC_HeartBeat"));
        System.out.println("s2 = " + opcUaTemplate.readItem("s2", "PLC_HeartBeat"));
    }
}
