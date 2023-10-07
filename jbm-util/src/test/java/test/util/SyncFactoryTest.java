package test.util;

import cn.hutool.core.thread.ThreadUtil;
import com.jbm.util.thread.SyncBean;
import com.jbm.util.thread.SyncFactory;

public class SyncFactoryTest {

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
