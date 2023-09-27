package jbm.framework.boot.autoconfigure.taskflow2.test.dataflow;

import com.ebay.bascomtask.core.Orchestrator;
import jbm.framework.boot.autoconfigure.taskflow2.test.dataflow.process.ProcessA;
import jbm.framework.boot.autoconfigure.taskflow2.test.dataflow.process.ProcessB;
import jbm.framework.boot.autoconfigure.taskflow2.test.dataflow.source.JsonDataFlowSource;
import org.junit.jupiter.api.Test;
public class DataFlowTest {

    private Orchestrator $ = Orchestrator.create();

    @Test
    public void test() {

        JsonDataFlowSource source = new JsonDataFlowSource();
        ProcessA processA = new ProcessA();
        ProcessB processB = new ProcessB();
        $.activate(source.load()).thenApply(processA::process).thenApply(s-> $.fn(processB.process(s))).thenAccept(System.out::println);

    }

}
