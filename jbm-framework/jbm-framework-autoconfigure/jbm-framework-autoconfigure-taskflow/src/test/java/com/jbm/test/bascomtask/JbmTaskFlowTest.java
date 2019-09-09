package com.jbm.test.bascomtask;

import com.ebay.bascomtask.annotations.Work;
import com.google.common.collect.Lists;
import com.jbm.test.model.ClassRoom;
import com.jbm.test.model.Student;
import jbm.framework.boot.autoconfigure.taskflow.useage.PathTask;
import org.junit.Test;

import java.util.List;

public class JbmTaskFlowTest extends PathTaskTestBase {
    public List<Object> sources() {
        List<Object> list = Lists.newArrayList();
        Student student = new Student();
        student.setAge(10);
        student.setName("小明");
        list.add(student);
        Student student2 = new Student();
        student2.setAge(18);
        student2.setName("小王");
        list.add(student2);

        ClassRoom classRoom = new ClassRoom();
        classRoom.setClassName("一班");
        list.add(classRoom);
        ClassRoom classRoom1 = new ClassRoom();
        classRoom1.setClassName("二班");
        list.add(classRoom1);
        return list;
    }

    @Test
    public void testTask() {
        JbmTestTask jbmTestTask = new JbmTestTask();
//        jbmTestTask.init();
//        jbmTestTask.execute2(sources());
        jbmTestTask.execute2(sources());
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
