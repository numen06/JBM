package service.test.event.remote;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.text.MessageFormat;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Log4jConfigurer;

import com.td.framework.service.support.RemoteEventSupport;
import com.td.util.TimeUtil;

import service.test.event.WalkEvent;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = { "classpath:applicationContext-event.xml" })
public class RemoteEventTest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static {
		try {
			Log4jConfigurer.initLogging("classpath:log4j.xml");
		} catch (FileNotFoundException ex) {
			System.err.println("Cannot Initialize log4j");
		}
	}
	@Autowired
	private RemoteEventSupport remoteEventSupport;

//	@Test
	public void test() throws Exception {
//		remoteEventSupport.listener("tess");
		for (int i = 0; i < 20; i++) {
			Thread.sleep(10);
			boolean res = remoteEventSupport.postEvent(new WalkEvent(this, TimeUtil.getBeforeDay(i)));
			System.out.println(MessageFormat.format("-----发出信号{0},{1}-----", i, res));
		}
		Thread.sleep(10000);
	}
}
