package test.batch;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueryTest extends TestCase {

    public void testQuery() {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(10);
// 假设队列中有15个元素
        ThreadUtil.execAsync(() -> {
            while (true) {
                int i = 0;
                i ++;
                queue.put(i);
                ThreadUtil.safeSleep(1000);
            }
        });
// 将所有元素转移到ArrayList中
        while (true) {
            List<Integer> list = new ArrayList<>();
            queue.drainTo(list, 1);
            Console.log( "list: {}", list);
            ThreadUtil.safeSleep(1000);
        }
    }
}
