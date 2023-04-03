package com.jbm.framework.opcua;

import cn.hutool.core.util.NumberUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.jbm.framework.opcua.attribute.OpcBean;
import com.jbm.framework.opcua.attribute.OpcPoint;
import com.jbm.framework.opcua.event.ValueChanageEvent;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.api.UaSession;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
@NoArgsConstructor
public class OpcUaClientBean extends AbstractScheduledService {

    private String deviceId;
    private OpcBean opcBean;
    private OpcUaSource opcUaSource;
    private OpcUaClient opcUaClient;
    private Map<String, OpcPoint> points = Maps.newConcurrentMap();
    private Map<String, ValueChanageEvent> subscriptionPoints = Maps.newConcurrentMap();
    private LoadingCache<String, NodeId> nodeIdLoadingCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, NodeId>() {
                @Override
                public NodeId load(String pointName) throws Exception {
                    OpcPoint point = findPoint(pointName);
                    int namespace = point.getNamespace();
                    String tag = point.getTagName();
                    NodeId nodeId;
                    if (NumberUtil.isInteger(tag)) {
                        nodeId = new NodeId(namespace, NumberUtil.parseInt(tag));
                    } else {
                        nodeId = new NodeId(namespace, tag);
                    }
                    return nodeId;
                }
            });

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

    public NodeId getNodeId(String pointName) throws ExecutionException {
        return this.nodeIdLoadingCache.get(pointName);
    }

    @Override
    protected void startUp() throws Exception {
        //add
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

