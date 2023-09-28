package test.flow.source;

import cn.hutool.core.date.DateTime;
import com.alibaba.fastjson.JSON;
import test.flow.usage.BaseData;

import java.util.concurrent.CompletableFuture;

public class JsonDataFlowSource implements FlowSource {

      public CompletableFuture<String> load() {
            BaseData baseData = new BaseData();
            baseData.setA(1.0);
            baseData.setB(2.0);
            baseData.setC(3.0);
            baseData.setTime(DateTime.now());
            return complete(JSON.toJSONString(baseData));
      }
}
