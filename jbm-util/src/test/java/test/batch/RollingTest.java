package test.batch;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.jbm.util.batch.ActionBean;
import com.jbm.util.batch.RollingTask;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import test.entity.Student;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public class RollingTest {

    private final RollingTask<Long> countWithTime = RollingTask.createRollingTask(5L, TimeUnit.SECONDS, new Function<ActionBean<Long>, Long>() {

        @Override
        public Long apply(ActionBean<Long> actionBean) {
            log.info("消息队列最近5分钟处理日志:{}", actionBean.getCurrQuantity());
            return actionBean.getObj();
        }
    });


    @Test
    public void test2() {
        for (int i = 0; i < 20; i++) {
            countWithTime.offer(RandomUtil.randomLong());
            ThreadUtil.safeSleep(1000);
        }
    }

}
