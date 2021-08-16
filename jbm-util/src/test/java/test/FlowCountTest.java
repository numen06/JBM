package test;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import com.jbm.util.TimeUtils;
import com.jbm.util.flow.FlowCount;
import org.junit.Test;

import java.util.concurrent.*;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-07-15 19:17
 **/
public class FlowCountTest {

    @Test
    public void testFlowSECONDS() throws InterruptedException {
        FlowCount flowStatistics = new FlowCount(3, TimeUnit.SECONDS);
        ExecutorService pool = ThreadUtil.newSingleExecutor();
        pool.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        flowStatistics.add();
                        TimeUnit.MILLISECONDS.sleep(RandomUtil.randomInt(10,100));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        });
        ScheduledExecutorService schePool = Executors.newScheduledThreadPool(1);
        schePool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                System.out.println("当前访问量:" + flowStatistics.nowFlow());
                System.out.println("最后一次访问量:" + flowStatistics.lastFlow());
            }

        }, 1000, 1000, TimeUnit.MILLISECONDS);
        pool.awaitTermination(1, TimeUnit.HOURS);
    }
}
