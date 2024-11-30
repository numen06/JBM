package test.batch;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.jbm.util.batch.DelayKeyUpdateTask;
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
        DelayUpdateTask<Integer> task = new DelayUpdateTask<>(5, TimeUnit.SECONDS, (o) -> {
            log.info("update:{}", o);
        },null);
        for (int i = 0; i < 100; i++) {
            int x = RandomUtil.randomInt(1, 3);
            task.delayUpdate(x, (obj) -> {
                log.info("test2 commit:{}", obj);
            });
            ThreadUtil.safeSleep(500);
        }
    }

    @Test
    public void testMap() {
        DelayKeyUpdateTask<Integer> task = new DelayKeyUpdateTask<>(5, TimeUnit.SECONDS, (o) -> {
            log.info("update:{}", o);
        },null);
        for (int i = 0; i < 100; i++) {
            int x = RandomUtil.randomInt(1, 3);
            task.delayUpdate(()->{
                return Pair.of(x, x);
            }, (obj) -> {
                log.info("testMap commit:{}", obj);
            });
            ThreadUtil.safeSleep(1000);
        }
    }

}
