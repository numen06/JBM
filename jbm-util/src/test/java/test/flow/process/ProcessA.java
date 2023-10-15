package test.flow.process;

import com.alibaba.fastjson.JSON;
import com.jbm.util.flow.FlowProcess;
import test.flow.usage.BaseData;

import java.util.concurrent.CompletableFuture;

public class ProcessA implements FlowProcess<String, BaseData> {


    @Override
    public CompletableFuture<BaseData> process(String source) {
        return complete(JSON.parseObject(source, BaseData.class));
    }


}
