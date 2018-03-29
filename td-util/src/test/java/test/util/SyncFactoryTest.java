package test.util;

import com.td.util.thread.SyncBean;
import com.td.util.thread.SyncFactory;

import jodd.util.ThreadUtil;
import junit.framework.TestCase;

public class SyncFactoryTest extends TestCase {

	public void testTheadSync() throws Exception {
		final SyncBean sync = SyncFactory.getInstance().createSyncBean(2);
		Thread th1 = new Thread(new Runnable() {
			@Override
			public void run() {
				ThreadUtil.sleep(3000);
				System.out.println("sleep 3000");
				sync.countDown();
			}
		});
		Thread th2 = new Thread(new Runnable() {

			@Override
			public void run() {
				ThreadUtil.sleep(1000);
				System.out.println("sleep 1000");
				sync.countDown();
			}
		});
		th1.start();
		th2.start();
		sync.await();
		System.out.println("run down");
	}
}
