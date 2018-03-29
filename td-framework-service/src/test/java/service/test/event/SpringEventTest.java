package service.test.event;

import java.io.FileNotFoundException;
import java.util.Date;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Log4jConfigurer;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = { "classpath:applicationContext-event.xml" })
public class SpringEventTest {
	@Autowired
	private WalkAware walkAware;

	static {
		try {
			Log4jConfigurer.initLogging("classpath:log4j.xml");
		} catch (FileNotFoundException ex) {
			System.err.println("Cannot Initialize log4j");
		}
	}

//	@Test	
	public void test() throws Exception {
		for (int i = 0; i < 10; i++) {
			// Thread.sleep(1000);
			walkAware.nextTime(new Date());
		}
	}
}
