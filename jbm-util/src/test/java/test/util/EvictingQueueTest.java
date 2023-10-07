package test.util;

import com.google.common.collect.EvictingQueue;

public class EvictingQueueTest {

    public static void main(String[] args) {
        EvictingQueue<String> queue = EvictingQueue.create(1);
        for (int i = 0; i < 100; i++) {
            queue.offer(i + "");
            System.out.println(queue.peek());
        }

    }

}
