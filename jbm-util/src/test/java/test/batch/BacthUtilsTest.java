package test.batch;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.util.batch.BatchTask;
import com.jbm.util.batch.BatchUtils;
import com.jbm.util.batch.DebounceMethod;
import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

public class BacthUtilsTest extends TestCase {


    public void testDebounceCall() {
        DebounceMethod debounce = BatchUtils.createDebounceMethod(1, TimeUnit.SECONDS);
        // 假设你在一个按钮点击监听器或其他频繁触发的地方调用这个方法
        long delay = 1; // 延迟时间，单位毫秒


        //通过10个进程同时并发请求测试
        for (int i = 0; i < 10; i++) {
            System.out.println(StrUtil.format("debounceCall {}...", i));
            int finalI = i;
            debounce.debounceCall(() -> {
                System.out.println(StrUtil.format("This is the [{}] call after {} s of inactivity.", finalI, delay));
            });
        }

        // 在程序结束前确保关闭debounce的线程池
        // debounce.shutdown();
        ThreadUtil.sleep(TimeUnit.SECONDS.toMillis(delay * 3));
        System.out.println("end...");
    }


    public void testBatchTask() {
        BatchTask<String> batchTask = BatchTask.createBatchTask(20,batchs -> {
            System.out.println("message size:" + batchs.size());
        });

        ThreadUtil.execAsync(() -> {
            while (true) {
                batchTask.add("我是消息:" + DateUtil.now());
                ThreadUtil.sleep(20);
            }
        });
        ThreadUtil.sleep(TimeUnit.SECONDS.toMillis(10));
    }



}