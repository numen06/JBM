package com.jbm.cluster.logs.entity;

import com.jbm.cluster.api.model.gateway.GatewayLogInfo;
import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2021-05-06 16:52
 **/
@Data
@Document("gateway_logs")
@CompoundIndexes({
        @CompoundIndex(name = "requestTime_Index", def = "{'requestTime':-1}"),
        @CompoundIndex(name = "requestTime_Index2", def = "{'requestTime':-1,'path':1}")
})
public class GatewayLogs extends GatewayLogInfo {


}
