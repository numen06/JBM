package test.util;

import com.google.common.collect.Queues;
import com.jbm.util.bean.queue.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SortingQueueTest {

    public static void main(String[] args) {
        final BlockingQueue<String> queue = Queues.newLinkedBlockingDeque();
        queue.add("1");
        queue.add("2");
        queue.add("3");

        Executors.newScheduledThreadPool(10).scheduleAtFixedRate(new Runnable() {
            public void run() {
                queue.add(System.currentTimeMillis() + "");
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        SortingQueue<Integer, String> is = new SortingQueue<Integer, String>(queue, new ForwardFilter<Integer, String>() {
            @Override
            public ForwardKey<Integer> filter(final String obj) {
                ForwardKey<Integer> key = new ForwardKey<Integer>(obj.length());
                return key;
            }
        });
        is.addForward(new AbstractForward<Integer, String>() {

            @Override
            public Integer getKey() {
                return 13;
            }

            @Override
            public void forword(String element, BlockingQueue<String> forwordQueue) {
                System.out.println("forword:" + element);
                this.setForwardState(ForwardState.DESTROY);
            }

        });
        is.sorting();
        System.out.println("end1");
        // is.stop();
        System.out.println("end2");
    }
}