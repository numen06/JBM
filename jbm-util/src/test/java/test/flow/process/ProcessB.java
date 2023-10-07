package test.flow.process;

import com.alibaba.fastjson.JSON;
import test.flow.usage.BaseData;

import java.util.concurrent.CompletableFuture;

public class ProcessB implements FlowProcess<BaseData, String> {


    @Override
    public CompletableFuture<String> process(BaseData source) {
        return complete(JSON.toJSONString(source));
    }

}
