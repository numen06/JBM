package com.jbm.framework.opcua;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.jbm.framework.opcua.attribute.OpcPoint;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
@NoArgsConstructor
public class OpcUaClientBean extends AbstractScheduledService {

    private String deviceId;
    private OpcUaSource opcUaSource;
    private OpcUaClient opcUaClient;
    private Map<String, OpcPoint> points = new HashMap<>();

    public OpcUaClientBean(String deviceId, OpcUaClient opcUaClient) {
        this.deviceId = deviceId;
        this.opcUaClient = opcUaClient;
        this.startAsync();
    }

    public OpcPoint findPoint(String pointName) {
        if (!points.containsKey(pointName)) {
            log.warn("没有发现点位:{}", pointName);
            return null;
        }
        return this.points.get(pointName);
    }

    @Override
    protected void runOneIteration() {
        try {
            if (!this.isRunning()) {
                log.info("OPCUA客户端[{}]开始启动执行守护线程", deviceId);
            }
            // 获取OPC UA服务器的数据
            opcUaClient.connect().get();
        } catch (Exception e) {
            log.warn("OPCUA客户端[{}]连接[{}]失败:{}", deviceId, opcUaSource.getUrl(), e.getMessage());
        }
    }

    @Override
    protected Scheduler scheduler() {
        //30秒监测一次PLC状态
        return Scheduler.newFixedRateSchedule(30, 30, TimeUnit.SECONDS);
    }
}
