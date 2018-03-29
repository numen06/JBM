package com.td.autoconfigure.logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SampleTestApplication.class)
public class LoggerTest {

	@Test
	public void testKafkaLogger() {
		System.out.println("testKafkaLogger");
	}

}
