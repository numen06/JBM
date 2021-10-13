package com.jbm.framework.opcua;

import com.jbm.framework.opcua.attribute.OpcPoint;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

import java.util.HashMap;
import java.util.Map;

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
        return this.points.get(pointName);
    }

}
