package test.flow.process;

import com.alibaba.fastjson.JSON;
import com.jbm.util.flow.AbstractFlowProcess;
import com.jbm.util.flow.FlowProcess;
import test.flow.usage.BaseData;

import java.util.concurrent.CompletableFuture;

public class ProcessA extends AbstractFlowProcess<String, BaseData> {


    @Override
    public BaseData doProcess(String source) {
        return JSON.parseObject(source, BaseData.class);
    }


}
