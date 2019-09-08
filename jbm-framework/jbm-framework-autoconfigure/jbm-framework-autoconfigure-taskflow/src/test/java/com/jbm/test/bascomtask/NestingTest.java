package com.jbm.test.bascomtask;

import com.ebay.bascomtask.annotations.Work;
import com.jbm.test.bascomtask.PathTaskTestBase;
import jbm.framework.boot.autoconfigure.taskflow.useage.PathTask;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class NestingTest extends PathTaskTestBase {

    @Test
    public void testMultipleExecute() {
        class A extends PathTask {
            @Work
            public void exec() {
                got();
            }
        }
        class B extends PathTask {
            @Work
            public void exec() {
                got();
            }
        }
        B b = new B();
        A a = new A();
        PathTask taskA = track.work(a);
        PathTask taskB = track.work(b);
        verify(1);

        class C extends PathTask {
            @Work
            public void exec(A a) {
                got(a);
            }
        }
        class D extends PathTask {
            @Work
            public void exec(C c) {
                got(c);
            }
        }
        C c = new C();
        D d = new D();

        PathTask taskC = track.work(c).exp(a);
        PathTask taskD = track.work(d).exp(c);
        verify(1);

        assertTrue(taskC.followed(taskA));
        assertTrue(taskD.followed(taskC));
    }

    @Test
    public void testSimpleNested() {
        class A extends PathTask {
            @Work
            public void exec() {
                class B extends PathTask {
                    @Work
                    public void exec() {
                        got();
                    }
                }
                B b = new B();
                PathTask taskB = track.work(b);
            }
        }
        A a = new A();
        PathTask taskA = track.work(a);
    }

}
