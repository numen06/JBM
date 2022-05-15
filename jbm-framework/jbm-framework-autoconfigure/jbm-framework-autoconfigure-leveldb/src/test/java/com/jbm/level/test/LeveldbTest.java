package com.jbm.level.test;

import java.text.MessageFormat;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.level.LevelKeyValueTemplate;
import org.springframework.data.level.LevelTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import jbm.framework.boot.autoconfigure.level.LevelAutoConfiguration;

@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@SpringBootTest(classes = {LevelAutoConfiguration.class})
public class LeveldbTest {

    @Autowired
    private LevelTemplate<Object, Object> levelTemplate;

    @Autowired
    private LevelKeyValueTemplate levelKeyValueTemplate;

    private final String dbName = "test";
    private final String mKey = "test";

    @Test
    public void exampleTest1() {
        levelTemplate.put(dbName, mKey, "123");
        System.out.println(levelTemplate.get(dbName, mKey));
    }

    @Test
    public void exampleTest2() throws InterruptedException {

        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 1000; i++) {
            // final String index = i+"";
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    String key = UUID.randomUUID().toString();
                    String value = UUID.randomUUID().toString();
                    levelTemplate.put(dbName, key, value);
                    System.out.println(MessageFormat.format("key:{0},value:{1}", key, levelTemplate.get(dbName, key)));
                }
            });
        }
        // 启动一次顺序关闭，执行以前提交的任务，但不接受新任务。
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("-----");
    }

    @Test
    public void exampleTest3() {
        Set<Object> keys = levelTemplate.keys(dbName);
        for (Object key : keys) {
            System.out.println("delete key:" + key);
            levelTemplate.delete(dbName, key);
        }
        System.out.println(levelTemplate.get(dbName, mKey));
    }

    @Test
    public void exampleKeysTest() throws Exception {
        UserTestBean bean = UserTestBean.newBean();
        System.out.println(levelKeyValueTemplate.insert(UserTestBean.newBean()));
        System.out.println(levelKeyValueTemplate.insert(bean));
        System.out.println(levelKeyValueTemplate.count(UserTestBean.class));
        System.out.println(levelKeyValueTemplate.findById(bean.getId(), UserTestBean.class));
        System.out.println(levelKeyValueTemplate.delete(bean));
        System.out.println(levelKeyValueTemplate.count(UserTestBean.class));
        // levelKeyValueTemplate.delete(UserTestBean.class);
        // System.out.println(levelKeyValueTemplate.count(UserTestBean.class));
        levelKeyValueTemplate.destroy();
        System.out.println(levelKeyValueTemplate.count(UserTestBean.class));
        levelKeyValueTemplate.destroy();
    }

}
