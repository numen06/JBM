package com.jbm.framework.opcua;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jbm.framework.opcua.attribute.OpcPoint;
import com.jbm.framework.opcua.attribute.OpcPointsRead;
import com.jbm.framework.opcua.attribute.ValueType;
import com.jbm.framework.opcua.event.ValueChanageEvent;
import com.jbm.framework.opcua.key.KeyLoader;
import com.jbm.framework.opcua.model.NodeEventBean;
import com.jbm.framework.opcua.util.DriverUtils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

@Service
@Slf4j
public class OpcUaTemplate {

    private Map<String, OpcUaClientBean> clientMap = new ConcurrentHashMap<>(16);

    private Map<String, NodeEventBean> NODE_EVENTS = new ConcurrentHashMap<>();

    @Autowired
    private ApplicationContext applicationContext;


    public void loadClients(Map<String, OpcUaSource> opcUaSourceMap) {
        try {
            for (String deviceId : opcUaSourceMap.keySet()) {
                OpcUaSource source = opcUaSourceMap.get(deviceId);
                OpcUaClient opcUaClient = this.getOpcUaClient(deviceId, source);
                OpcUaClientBean opcUaClientBean = new OpcUaClientBean();
                opcUaClientBean.setOpcUaSource(source);
                opcUaClientBean.setPoints(this.loadPoints(source));
                opcUaClientBean.setDeviceId(deviceId);
                opcUaClientBean.setOpcUaClient(opcUaClient);
                clientMap.put(deviceId, opcUaClientBean);
            }
        } catch (Exception e) {
            log.error("读取OPCUA设备失败", e);
            throw e;
        }
    }


    public Map<String, OpcPoint> loadPoints(OpcUaSource opcUaSource) {
        if (StrUtil.isNotBlank(opcUaSource.getPointFile())) {
            OpcPointsRead opcPointsRead = new OpcPointsRead();
            return opcPointsRead.readPoints(opcUaSource.getPointFile());
        }
        return Maps.newConcurrentMap();
    }

    /**
     * 获取所有配置的设备
     *
     * @return
     */
    public List<String> getDeviceIds() {
        return Lists.newArrayList(clientMap.keySet());
    }


    public OpcUaClient getOpcUaClient(String deviceId) {
        return this.getOpcUaClient(deviceId, null);
    }

    /**
     * Get Opc Ua Client
     *
     * @param deviceId   Device Id
     * @param driverInfo Driver Info
     * @return OpcUaClient
     * @throws UaException UaException
     */
    public OpcUaClient getOpcUaClient(String deviceId, OpcUaSource driverInfo) {
        OpcUaClient opcUaClient = null;
        if (clientMap.containsKey(deviceId))
            return clientMap.get(deviceId).getOpcUaClient();
        try {
            KeyLoader loader = new KeyLoader().load(Paths.get(FileUtil.getTmpDirPath()));
            if (null == opcUaClient) {
                try {
                    opcUaClient = OpcUaClient.create(
                            driverInfo.getUrl(),
                            endpoints -> endpoints.stream().findFirst(),
                            configBuilder -> configBuilder
                                    .setIdentityProvider(new AnonymousProvider())
                                    .setCertificate(loader.getClientCertificate())
                                    .setRequestTimeout(uint(5000))
                                    .build()
                    );
                    clientMap.put(deviceId, new OpcUaClientBean(deviceId, opcUaClient));
                } catch (UaException e) {
                    log.error("get opc ua client error: {}", e.getMessage());
                    clientMap.entrySet().removeIf(next -> next.getKey().equals(deviceId));
                }
            }
        } catch (Exception e) {
            log.error("get opc ua client error: {}", e.getMessage());
        }
        if (!clientMap.containsKey(deviceId)) {
            log.error("not found opcua client in cache");
            return null;
        }
        return clientMap.get(deviceId).getOpcUaClient();
    }

    public <T> T readItem(String deviceId, String pointName, Class<T> dataType) throws Exception {
        return JSON.parseObject(this.readItem(deviceId, pointName), dataType);
    }

    public String readItem(String deviceId, String pointName) throws Exception {
        OpcUaClientBean opcUaClientBean = clientMap.get(deviceId);
        OpcPoint point = opcUaClientBean.findPoint(pointName);

        return this.readItem(deviceId, point);
    }

    public String readItem(String deviceId, OpcPoint point) throws Exception {
        int namespace = point.getNamespace();
        String tag = point.getTagName();

        NodeId nodeId = new NodeId(namespace, tag);
        CompletableFuture<String> value = new CompletableFuture<>();
        OpcUaClient client = getOpcUaClient(deviceId);
        log.debug("start read point(ns={};s={})", namespace, tag);
        client.connect().get();
        client.readValue(0.0, TimestampsToReturn.Both, nodeId).thenAccept(dataValue -> {
            try {
                value.complete(dataValue.getValue().getValue().toString());
            } catch (Exception e) {
                log.error("accept point(ns={};s={}) value error", namespace, tag, e);
            }
        });
        String rawValue = value.get(3, TimeUnit.SECONDS);
        log.debug("end read point(ns={};s={}) value: {}", namespace, tag, rawValue);
        return rawValue;

    }

    public void writeItem(String deviceId, String pointName, Object value) {
        OpcUaClientBean opcUaClientBean = clientMap.get(deviceId);
        OpcPoint opcPoint = opcUaClientBean.findPoint(pointName);
        opcPoint.setValue(value);
        this.writeItem(deviceId, opcPoint);
    }


    /**
     * Write Opc Ua Point Value
     *
     * @param deviceId Device Id
     * @param point    OpcPoint Info
     * @throws UaException          UaException
     * @throws ExecutionException   ExecutionException
     * @throws InterruptedException InterruptedException
     */
    public void writeItem(String deviceId, OpcPoint point) {
        OpcUaClient client;
        try {
            log.debug("OPCUA写入点位:{}", JSON.toJSONString(point));
            int namespace = point.getNamespace();
            String tag = point.getTagName();
            NodeId nodeId = new NodeId(namespace, tag);
            client = getOpcUaClient(deviceId);
            client.connect().get();
            StatusCode statusCode = StatusCode.GOOD;
            DataValue dataValue = this.convertData(point);
            statusCode = client.writeValue(nodeId, dataValue).get();
            if (!statusCode.isGood()) {
                throw new RuntimeException(statusCode.toString());
            }
        } catch (Exception e) {
            log.error("Opc Ua Point Write Error", e);
        }
    }

    public <T extends ValueChanageEvent> void subscribeItem(String deviceId, String pointName, Class<T> callBackEvent) {
        OpcUaClientBean opcUaClientBean = clientMap.get(deviceId);
        OpcPoint opcPoint = opcUaClientBean.findPoint(pointName);
        this.subscribeItem(deviceId, opcPoint, callBackEvent);
    }


//    public <T extends ValueChanageEvent> void putEvent(String deviceId, NodeId point, Class<T> callBackEvent) {
//        String key = String.format("%s-%s-%s", deviceId, point.getNamespaceIndex(), point.getIdentifier());
//        if (this.nodeEvents.containsKey(key)) {
//            this.nodeEvents.get(key).add(callBackEvent);
//        } else {
//            this.nodeEvents.put(key, Lists.newArrayList(callBackEvent));
//        }
//    }

    public <T extends ValueChanageEvent> void putEvent(String deviceId, UaMonitoredItem item, Class<T> callBackEvent) {
        String key = item.getMonitoredItemId().toString();
        if (this.NODE_EVENTS.containsKey(key)) {
            NodeEventBean nodeEventBean = this.NODE_EVENTS.get(key);
            nodeEventBean.getEventClassList().add(callBackEvent);
        } else {
            NodeEventBean nodeEventBean = new NodeEventBean(deviceId, Lists.newArrayList(callBackEvent));
            this.NODE_EVENTS.put(key, nodeEventBean);
        }
    }


    /**
     * 创建监听器
     *
     * @param client
     * @return
     */
    public UaSubscription getSubscription(OpcUaClient client) throws ExecutionException, InterruptedException {
        UaSubscription subscription = CollUtil.getFirst(client.getSubscriptionManager().getSubscriptions());
        if (subscription == null)
            subscription = client.getSubscriptionManager().createSubscription(1000.0).get();
        return subscription;
    }


    /**
     * 订阅节点
     */
    public <T extends ValueChanageEvent> void subscribeItem(String deviceId, OpcPoint point, Class<T> callBackEvent) {
        OpcUaClient client;
        try {
            log.info("OPCUA订阅点位:{}", JSON.toJSONString(point));
            int namespace = point.getNamespace();
            String tag = point.getTagName();
            NodeId nodeId = new NodeId(namespace, tag);
            client = getOpcUaClient(deviceId);
            client.connect().get();
            List<UaMonitoredItem> items = this.createItemMonitored(client, nodeId);
//            //循环设置回调事件
            for (UaMonitoredItem item : items) {
                this.putEvent(deviceId, item, callBackEvent);
            }
        } catch (Exception e) {
            log.error("Opc Ua Point Write Error", e);
        }
    }

    private List<UaMonitoredItem> createItemMonitored(OpcUaClient client, NodeId nodeId) throws ExecutionException, InterruptedException {
        //创建发布间隔1000ms的订阅对象
        UaSubscription subscription = this.getSubscription(client);
        MonitoringParameters parameters = new MonitoringParameters(
                uint(subscription.getMonitoredItems().size() + 1),
                1000.0,
                null,
                uint(10),
                true
        );
        List<MonitoredItemCreateRequest> requests = Lists.newArrayList();
        ReadValueId readValueId = new ReadValueId(nodeId, AttributeId.Value.uid(), null, null);
        //创建监控item, 第一个为Reporting mode
        MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters);
        requests.add(request);
        List<UaMonitoredItem> items = subscription.createMonitoredItems(
                TimestampsToReturn.Both,
                requests,
                (item, id) -> item.setValueConsumer(this::onSubscriptionValue)
        ).get();
        log.info("监听器[{}]监听数量:{}", subscription.getSubscriptionId(), subscription.getMonitoredItems().size());
        return items;
    }


    private void onSubscriptionValue(UaMonitoredItem item, DataValue value) {
        log.info("OPC数据变化回调：subscription value received: item={}, value={}",
                item.getReadValueId().getNodeId(), value.getValue());
        String key = item.getMonitoredItemId().toString();
        if (this.NODE_EVENTS.containsKey(key)) {
            NodeEventBean nodeEventBean = this.NODE_EVENTS.get(key);
            for (Class<ValueChanageEvent> eventClass : nodeEventBean.getEventClassList()) {
                ValueChanageEvent valueChanageEvent = ReflectUtil.newInstance(eventClass, item, value);
                valueChanageEvent.setDeviceId(nodeEventBean.getDeviceId());
                applicationContext.publishEvent(valueChanageEvent);
            }
        }
    }

    private DataValue convertData(OpcPoint point) {
        ValueType valueType = ValueType.valueOf(point.getDataType().toUpperCase());
        return this.convertData(valueType, point.getValue());
    }

    /**
     * 转换数据类型
     *
     * @param valueType
     * @param value
     * @return
     */
    private DataValue convertData(ValueType valueType, Object value) {
        return buildDataValue(DriverUtils.value(valueType, value.toString()));
    }


    private DataValue buildDataValue(Object val) {
        return new DataValue(new Variant(val), null, null);
    }

}
