package com.jbm.framework.plugin.emqttd.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-11-20 19:11
 **/
@SpringBootApplication
@ComponentScan("com.jbm.framework")
public class EmqttdApplicationTest {


    public static void main(String[] args) {
        SpringApplication.run(EmqttdApplicationTest.class, args);
    }

}
