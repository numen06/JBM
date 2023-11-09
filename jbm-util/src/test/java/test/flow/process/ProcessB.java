package test.flow.process;

import com.alibaba.fastjson.JSON;
import com.jbm.util.flow.AbstractFlowProcess;
import test.flow.usage.BaseData;

public class ProcessB extends AbstractFlowProcess<BaseData, String> {


    @Override
    public String doProcess(BaseData source) {
        return JSON.toJSONString(source);
    }

}
