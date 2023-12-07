package test.flow;

import com.ebay.bascomtask.core.Orchestrator;
import org.junit.jupiter.api.Test;
import test.flow.process.ProcessA;
import test.flow.process.ProcessB;
import test.flow.process.ProcessC;
import test.flow.source.JsonDataFlowSource;

public class DataFlowTest {

    private Orchestrator $ = Orchestrator.create();

    @Test
    public void test() {
        JsonDataFlowSource source = new JsonDataFlowSource();
        ProcessA processA = new ProcessA();
        ProcessB processB = new ProcessB();
        ProcessC processC = new ProcessC();
        $.activate(source.load()).thenApply(json -> processA.process(json)).thenApply(s -> processB.process(s))
                .thenAccept(s -> processC.process(s));

    }

}
