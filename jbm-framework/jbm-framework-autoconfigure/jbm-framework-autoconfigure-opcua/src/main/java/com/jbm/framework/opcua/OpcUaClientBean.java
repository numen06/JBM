package com.jbm.framework.opcua;

import com.jbm.framework.opcua.attribute.OpcPoint;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Data
@NoArgsConstructor
public class OpcUaClientBean {

    private String deviceId;
    private OpcUaSource opcUaSource;
    private OpcUaClient opcUaClient;
    private Map<String, OpcPoint> points = new HashMap<>();

    public OpcUaClientBean(String deviceId, OpcUaClient opcUaClient) {
        this.deviceId = deviceId;
        this.opcUaClient = opcUaClient;
    }


    public OpcPoint findPoint(String pointName) {
        if (!points.containsKey(pointName)) {
            log.warn("没有发现点位:{}", pointName);
            return null;
        }
        return this.points.get(pointName);
    }

}
