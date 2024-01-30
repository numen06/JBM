package test.batch;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.jbm.util.batch.ActionBean;
import com.jbm.util.batch.BatchTask;
import com.jbm.util.batch.RollingTask;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import test.entity.Student;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class BatchTest {

    @Test
    public void test() {
        BatchTask<String> batchTask = new BatchTask(2L, TimeUnit.SECONDS,0,new Consumer<List>() {
            @Override
            public void accept(List list) {
                log.info("处理{}条数据", list.size());
            }
        });
        log.info("开始批处理添加");
        for (int i = 0; i < 100; i++) {
            int a = batchTask.offer(DateUtil.now());
            log.info("追加数量为:{}",a);
        }
        log.info("开始批处理多线程添加");
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        batchTask.offerOfWait(2, TimeUnit.SECONDS,DateUtil.now());
                    } catch (InterruptedException e) {
                        log.error("超时加入了",e);
                    }
                    ThreadUtil.safeSleep(10);
                }
            }
        });
        batchTask.awaitTerminated();

    }

    @Test
    public void test2() {


        RollingTask batchTask = RollingTask.createRollingTask(5L,TimeUnit.SECONDS,new Function<ActionBean<Student>,Student>() {
            @Override
            public Student apply(ActionBean<Student> rollingBean) {
                Student student = rollingBean.getObj();
                if(RandomUtil.randomInt(1,10)==2) {
                     throw  new RuntimeException("随机失败");
                }
                if(ObjectUtil.isNull(student)){
                    student = new Student();
                    student.setAge(0);
                }
                student.setAge(student.getAge()+1);
                log.info("处理{}条数据,单次循环数量为{}",student.getAge(),rollingBean.getCurrQuantity());
                return student;
            }
        });
        for (int i = 0; i < 100; i++) {
            batchTask.offer(DateUtil.now());
        }
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    batchTask.offer();
                    ThreadUtil.safeSleep(1000);
                }
            }
        });
        batchTask.awaitTerminated();

    }
}
