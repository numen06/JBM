package com.jbm.test.bascomtask;

import com.ebay.bascomtask.annotations.Work;
import com.ebay.bascomtask.main.Orchestrator;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

public class BascomTaskTest {
    class HelloTask {
        String getMessage() {
            return "Hello";
        }
    }

    class WorldTask {
        private String msg;

        String getMessage() {
            return msg;
        }

        @Work
        public void test() {
            this.msg = "World";
        }
    }


    class HelloWorldTask {


        private Orchestrator orc;

        public HelloWorldTask() {
        }

        public HelloWorldTask(Orchestrator orc) {
            this.orc = orc;
        }

        @Work
        public void exec() {
            orc.addWork(new HelloTask());
            orc.addWork(new WorldTask());
            orc.execute();
            System.out.println("Hello World");
        }
    }


    class ProxyTask {
        @Work
        public void exec(AtomicReference<String> helloTask, AtomicReference<HelloTask> worldTask) {
            System.out.println(helloTask.get() + " " + worldTask.get().getMessage());
        }
    }

    @Test
    public void proxyHelloWorld() {
        Orchestrator orc = Orchestrator.create();
        orc.addWork(new AtomicReference<String>("test"));
//        orc.addWork(new AtomicReference<HelloTask>(new HelloTask()));
        orc.addWork(new ProxyTask());
        orc.execute();  // Invokes tasks and waits all results are ready
    }

    class ConcatenatorTask {
        @Work
        public void exec(HelloTask helloTask, WorldTask worldTask) {
            System.out.println(helloTask.getMessage() + " " + worldTask.getMessage());
        }
    }


    @Test
    public void hasHelloWorld() {
        Orchestrator orc = Orchestrator.create();
//        orc.addWork(new HelloWorldTask(orc));
        orc.addWork(new ConcatenatorTask());
        orc.execute();  // Invokes tasks and waits all results are ready
    }


    @Test
    public void mutilTask() {
        Orchestrator orc = Orchestrator.create();
        orc.addWork(new HelloTask());
        orc.addWork(new WorldTask());
        orc.addWork(new ConcatenatorTask());
        orc.execute();
    }

    class A {
        @Work
        public void exec() {
            System.out.println(this.getClass().getSimpleName());
        }
    }

    class B {
        @Work
        public void exec() {
            System.out.println(this.getClass().getSimpleName());
        }
    }

    class C {
        @Work
        public void exec() {
            System.out.println(this.getClass().getSimpleName());
        }
    }

    class DependsOnB {
        @Work
        public void exec(B b) {
            System.out.println(this.getClass().getSimpleName());
        }
    }

    class DependsOnC {
        @Work
        public void exec(C c) {
            System.out.println(this.getClass().getSimpleName());
        }
    }

    class Chooser {
//        @Work public void exec(A a, B b, C c) {
//            if (a.someConditionIsTrue()) {
//                Orchestrator orc = Orchestrator.create();
//                orc.addWork(b);
//                orc.addWork(c);
//                orc.addWork(new DependsOnB());
//                orc.addWork(new DependsOnC());
//                orc.execute();
//            }
//        }
    }


    @Test
    public void bianPaiTask() {
        Orchestrator orc = Orchestrator.create();
        orc.addWork(new Chooser());
        orc.execute();
    }


    class more {
        @Work
        public boolean exec(A a) {
            System.out.println(a.getClass().getSimpleName());
            return false;
        }
    }

    class more2 {
        @Work
        public void exec(B b) {
            System.out.println(b.getClass().getSimpleName());
        }
    }

    @Test
    public void testMore() {
        Orchestrator orc = Orchestrator.create();
        orc.addIgnoreTaskMethods(new A());
        orc.addIgnoreTaskMethods(new B());
        orc.addWork(new more());
        orc.addWork(new more2());
        orc.execute();
    }

}


