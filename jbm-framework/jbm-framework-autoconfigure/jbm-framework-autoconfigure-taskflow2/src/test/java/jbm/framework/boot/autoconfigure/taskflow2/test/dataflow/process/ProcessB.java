package jbm.framework.boot.autoconfigure.taskflow2.test.dataflow.process;

import com.alibaba.fastjson.JSON;
import jbm.framework.boot.autoconfigure.taskflow2.test.dataflow.usage.BaseData;

import java.util.concurrent.CompletableFuture;

public class ProcessB implements FlowProcess<BaseData, String>{

    public CompletableFuture<String> process(CompletableFuture<BaseData> source) {
        return complete(JSON.toJSONString(source));
    }


}
