package com.jbm.framework.opcua;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.jbm.framework.opcua.attribute.OpcPoint;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.api.UaSession;

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
        this.awaitRunning();
    }

    public OpcPoint findPoint(String pointName) {
        if (!points.containsKey(pointName)) {
            log.warn("没有发现点位:{}", pointName);
            return null;
        }
        return this.points.get(pointName);
    }

    @Override
    protected void startUp() throws Exception {
        log.info("OPCUA客户端[{}]开始启动执行守护线程", deviceId);
        this.opcUaClient.addSessionActivityListener(new SessionActivityListener() {
            @Override
            public void onSessionActive(UaSession session) {
                log.info("PLC[{}][{}]建立链接成功", deviceId, session.getSessionName());
            }

            @Override
            public void onSessionInactive(UaSession session) {
                log.info("PLC[{}][{}]断开了链接", deviceId, session.getSessionName());
            }
        });
    }

    @Override
    protected void runOneIteration() {
        try {
            // 获取OPC UA服务器的数据
//            opcUaClient.connect().get();
//            log.warn("OPCUA客户端保持链接");
        } catch (Exception e) {
            log.warn("OPCUA客户端[{}]连接[{}]失败:{}", deviceId, opcUaSource.getUrl(), e.getMessage());
        }
    }

    @Override
    protected Scheduler scheduler() {
        //30秒监测一次PLC状态
        return Scheduler.newFixedRateSchedule(1, 5, TimeUnit.SECONDS);
    }
}
