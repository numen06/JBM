package com.jbm.autoconfigure.logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SampleTestApplication.class)
public class LoggerTest {

    @Test
    public void testKafkaLogger() {
        System.out.println("testKafkaLogger");
    }

}
