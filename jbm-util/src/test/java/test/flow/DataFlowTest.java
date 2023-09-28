package test.flow;

import com.alibaba.fastjson.JSON;
import com.ebay.bascomtask.core.Orchestrator;
import org.junit.jupiter.api.Test;
import test.flow.process.ProcessA;
import test.flow.process.ProcessB;
import test.flow.source.JsonDataFlowSource;
import test.flow.usage.BaseData;

public class DataFlowTest {

    private Orchestrator $ = Orchestrator.create();

    @Test
    public void test() {

        JsonDataFlowSource source = new JsonDataFlowSource();
        ProcessA processA = new ProcessA();
        ProcessB processB = new ProcessB();
        $.activate(source.load()).thenApply(json -> JSON.parseObject(json, BaseData.class)).thenApply(s -> JSON.toJSONString(s))
                .thenAccept(System.out::println);

    }

}
