package test.batch;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.jbm.util.batch.ActionBean;
import com.jbm.util.batch.BatchMapTask;
import com.jbm.util.batch.BatchTask;
import com.jbm.util.batch.RollingTask;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import test.entity.Student;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class BatchTest {

    @Test
    public void test() {
        BatchTask<String> batchTask = new BatchTask(1L, TimeUnit.SECONDS, 0, new Consumer<List>() {
            @Override
            public void accept(List list) {
                log.info("处理{}条数据", list.size());
            }
        });
        log.info("开始批处理添加");
        for (int i = 0; i < 10; i++) {
            int a = batchTask.offer(DateUtil.now());
            log.info("追加数量为:{}", a);
        }
        log.info("开始批处理多线程添加");
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    batchTask.offer(DateUtil.now());
                    ThreadUtil.safeSleep(200);
                }
            }
        });
        ThreadUtil.sleep(10, TimeUnit.SECONDS);
        log.info("结束");
    }

    /**
     * 测试错误
     */
    @Test
    public void test2() {
        RollingTask<Student> batchTask = RollingTask.createRollingTask(2L, TimeUnit.SECONDS, new Function<ActionBean<Student>, Student>() {
            @Override
            public Student apply(ActionBean<Student> rollingBean) {
                Student student = rollingBean.getObj();
                if (RandomUtil.randomInt(1, 10) == 2) {
                    throw new RuntimeException("随机失败");
                }
                if (ObjectUtil.isNull(student)) {
                    student = new Student();
                    student.setAge(0);
                }
                student.setAge(student.getAge() + 1);
                log.info("处理{}条数据,单次循环数量为{}", student.getAge(), rollingBean.getCurrQuantity());
                return student;
            }
        });
        for (int i = 0; i < 100; i++) {
            batchTask.offer(new Student());
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
        ThreadUtil.sleep(10, TimeUnit.SECONDS);
        log.info("结束");
    }

    @Test
    public void test3() {
        RollingTask<Student> batchTask = RollingTask.createRollingTask(5L, TimeUnit.SECONDS, new Function<ActionBean<Student>, Student>() {
            @Override
            public Student apply(ActionBean<Student> rollingBean) {
                Student student = rollingBean.getObj();
                if (ObjectUtil.isNull(student)) {
                    student = new Student();
                    student.setAge(0);
                }
                if (ObjectUtil.isNull(student.getAge())) {
                    student.setAge(0);
                }
                student.setAge(student.getAge() + 1);
                log.info("处理数据{},单次循环数量为{}", student, rollingBean.getCurrQuantity());
                return student;
            }
        });
        for (int i = 0; i < 100; i++) {
            batchTask.offerBlocking(new Student("张三", i, DateTime.now()));
        }
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Student student = new Student(IdUtil.getSnowflakeNextId());
                    student.setTime(DateTime.now());
                    batchTask.offerBlocking(student);
                    ThreadUtil.safeSleep(1000);
                }
            }
        });
        ThreadUtil.sleep(10, TimeUnit.SECONDS);
        log.info("结束");

    }

    @Test
    public void test4() {
        BatchMapTask<Student> batchTask = BatchMapTask.createBatchTask(5L, TimeUnit.SECONDS, new Consumer<Map<Integer, Student>>() {
            @Override
            public void accept(Map<Integer, Student> studentMap) {
                for (Student student : studentMap.values()) {
                    log.info("处理数据{}", student);
                }
            }
        });
        log.info("开始批处理添加");
        for (int i = 0; i < 100; i++) {
            int a = batchTask.offer(Student.newStudent());
            log.info("追加数量为:{}", a);
        }
        log.info("开始批处理多线程添加");
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    batchTask.offerBlocking(Student.newStudent());
                    ThreadUtil.sleep(100);
                }
            }
        });
        batchTask.awaitTerminated();

    }


    @Test
    public void testLogs() {
        RollingTask<Long> rollingTask = RollingTask.createRollingTask(5L, TimeUnit.SECONDS, new Function<ActionBean<Long>, Long>() {
            @Override
            public Long apply(ActionBean<Long> rollingBean) {
                log.info("5秒内处理{}条数据", rollingBean.getCurrQuantity());
                return rollingBean.getObj();
            }
        });
        BatchTask<String> batchTask = new BatchTask<>(5L, TimeUnit.SECONDS, 100, new Consumer<List<String>>() {
            @Override
            public void accept(List<String> logs) {
                log.info("处理了{}条日志", logs.size());
            }
        });
        while (true) {
            batchTask.offer(DateUtil.now());
            rollingTask.offer();
            ThreadUtil.safeSleep(10);
        }
    }

    @Test
    public void testLogs2() {
        final AtomicInteger offerCount = new AtomicInteger(0);
        BatchTask<String> batchTask = new BatchTask<>(5L, TimeUnit.SECONDS, 100, new Consumer<List<String>>() {
            @Override
            public void accept(List<String> logs) {
                log.info("处理了{}条日志", logs.size());
            }
        });
        while (true) {
            batchTask.offerBlocking(DateUtil.now());
            int cnt = offerCount.incrementAndGet();
            if (cnt % 100 == 0) {
                log.info("已放入 {} 条数据,批处理当前数量:{}", cnt, batchTask.getCurrQuantity());
            }
            ThreadUtil.safeSleep(10);
        }

    }
}
