package jbm.framework.boot.autoconfigure.taskflow2.test.dataflow;

import com.alibaba.fastjson.JSON;
import com.ebay.bascomtask.core.Orchestrator;
import jbm.framework.boot.autoconfigure.taskflow2.test.dataflow.process.ProcessA;
import jbm.framework.boot.autoconfigure.taskflow2.test.dataflow.process.ProcessB;
import jbm.framework.boot.autoconfigure.taskflow2.test.dataflow.source.JsonDataFlowSource;
import jbm.framework.boot.autoconfigure.taskflow2.test.dataflow.usage.BaseData;
import org.junit.jupiter.api.Test;

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
