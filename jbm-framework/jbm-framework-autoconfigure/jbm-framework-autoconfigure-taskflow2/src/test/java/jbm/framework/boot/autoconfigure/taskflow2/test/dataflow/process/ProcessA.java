package jbm.framework.boot.autoconfigure.taskflow2.test.dataflow.process;

import com.alibaba.fastjson.JSON;
import jbm.framework.boot.autoconfigure.taskflow2.test.dataflow.usage.BaseData;

import java.util.concurrent.CompletableFuture;

public class ProcessA implements FlowProcess<String, BaseData>{


    @Override
    public CompletableFuture<BaseData> process(String source) {
        return complete(JSON.parseObject(source, BaseData.class));
    }



}
