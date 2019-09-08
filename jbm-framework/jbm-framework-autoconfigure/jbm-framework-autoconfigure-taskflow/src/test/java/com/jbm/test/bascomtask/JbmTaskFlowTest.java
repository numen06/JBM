package com.jbm.test.bascomtask;

import com.ebay.bascomtask.annotations.Work;
import jbm.framework.boot.autoconfigure.taskflow.useage.PathTask;
import org.junit.Test;

public class JbmTaskFlowTest extends PathTaskTestBase {


    @Test
    public void testTask() {
        JbmTestTask jbmTestTask = new JbmTestTask();
        jbmTestTask.init();
        jbmTestTask.execute();
    }

    @Test
    public void test1SimpleActive() {
        class A extends PathTask {
            @Work
            public void exec() {
                got();
            }
        }
        A a = new A();
        PathTask taskA = track.work(a);
        taskA.getTaskInstance().getOrchestrator().execute();
        verify(0);
    }
}
