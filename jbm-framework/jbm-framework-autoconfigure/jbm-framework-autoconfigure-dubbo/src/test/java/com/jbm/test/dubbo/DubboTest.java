package com.jbm.test.dubbo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@SpringBootTest(classes = {DubboApp.class})
public class DubboTest {

    @Autowired
    private DubboProviderServcie dubboProviderServcie;

    @Test
    public void test() throws InterruptedException {
        dubboProviderServcie.test();
        Thread.sleep(10000);
    }
}
