package test.batch;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.jbm.util.batch.DelayUpdateTask;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

@Slf4j
public class DelayTest {

    @Test
    public void test() {
        DelayUpdateTask<String> task = new DelayUpdateTask<>(5, TimeUnit.SECONDS, (o) -> {
            log.info("update:{}", o);
        }, (o) -> {
            log.info("commit:{}", o);
        });
        for (int i = 0; i < 100; i++) {
            task.delayUpdate(IdUtil.fastSimpleUUID());
            ThreadUtil.safeSleep(1000);
        }
    }

    @Test
    public void test2() {
        DelayUpdateTask<String> task = new DelayUpdateTask<>(5, TimeUnit.SECONDS);
        for (int i = 0; i < 100; i++) {
            task.delayUpdate(IdUtil.fastSimpleUUID(), (obj) -> {
                log.info("d commit:{}", obj);
            });
            ThreadUtil.safeSleep(1000);
        }
    }

    @Test
    public void test3() {
        DelayUpdateTask<String> task = new DelayUpdateTask<>(5, TimeUnit.SECONDS);
        for (int i = 0; i < 100; i++) {
            task.delayRun(() -> log.info("d commit:"));
            ThreadUtil.sleep(RandomUtil.randomInt(4, 10),TimeUnit.SECONDS);
        }
    }


}
