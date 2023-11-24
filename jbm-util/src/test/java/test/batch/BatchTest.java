package test.batch;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.jbm.util.batch.BatchTask;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class BatchTest {

    @Test
    public void test() {
        BatchTask<String> batchTask = new BatchTask(5L, TimeUnit.SECONDS,10,new Consumer<List>() {
            @Override
            public void accept(List list) {
                log.info("处理{}条数据", list.size());
            }
        });
        for (int i = 0; i < 100; i++) {
            batchTask.offer(DateUtil.now());
        }
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    batchTask.offer(DateUtil.now());
                    ThreadUtil.safeSleep(10);
                }
            }
        });
        batchTask.awaitTerminated();

    }

    @Test
    public void test2() {
        BatchTask<String> batchTask = BatchTask.createBatchTask(new Consumer<List<String>>() {
            @Override
            public void accept(List list) {
                log.info("处理{}条数据", list.size());
            }
        });
        for (int i = 0; i < 100; i++) {
            batchTask.offer(DateUtil.now());
        }
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    batchTask.offer(DateUtil.now());
                    ThreadUtil.safeSleep(10);
                }
            }
        });
        batchTask.awaitTerminated();

    }
}
